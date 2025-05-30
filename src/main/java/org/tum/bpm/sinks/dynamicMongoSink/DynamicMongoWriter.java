package org.tum.bpm.sinks.dynamicMongoSink;

import org.apache.flink.annotation.Internal;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.functions.util.ListCollector;
import org.apache.flink.api.common.operators.MailboxExecutor;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.connector.mongodb.common.config.MongoConnectionOptions;
import org.apache.flink.connector.mongodb.sink.config.MongoWriteOptions;
import org.apache.flink.connector.mongodb.sink.writer.context.DefaultMongoSinkContext;
import org.apache.flink.connector.mongodb.sink.writer.context.MongoSinkContext;
import org.apache.flink.connector.mongodb.sink.writer.serializer.MongoSerializationSchema;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.groups.SinkWriterMetricGroup;
import org.apache.flink.util.Collector;
import org.apache.flink.util.FlinkRuntimeException;
import org.apache.flink.util.concurrent.ExecutorThreadFactory;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.TimeSeriesGranularity;
import com.mongodb.client.model.TimeSeriesOptions;
import com.mongodb.client.model.WriteModel;
import org.bson.BsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * This class is responsible for writing records to a MongoDB collection.
 *
 * @param <IN> The type of the input elements.
 */
@Internal
public class DynamicMongoWriter<IN> implements SinkWriter<MetaDocument<IN>> {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicMongoWriter.class);

    private final MongoConnectionOptions connectionOptions;
    private final MongoWriteOptions writeOptions;
    private final MongoSerializationSchema<IN> serializationSchema;
    private final MongoSinkContext sinkContext;
    private final MailboxExecutor mailboxExecutor;
    private final boolean flushOnCheckpoint;
    // private final List<WriteModel<BsonDocument>> bulkRequests = new
    // ArrayList<>();
    private final Map<String, List<WriteModel<BsonDocument>>> bulkRequests = new HashMap<>();
    // private final Collector<WriteModel<BsonDocument>> collector;
    private final Map<String, Collector<WriteModel<BsonDocument>>> collectors = new HashMap<>();

    private final Counter numRecordsOut;
    private final MongoClient mongoClient;
    private final long batchIntervalMs;
    private final int batchSize;

    private boolean checkpointInProgress = false;
    private volatile long lastSendTime = 0L;
    private volatile long ackTime = Long.MAX_VALUE;

    private transient volatile boolean closed = false;
    private transient ScheduledExecutorService scheduler;
    private transient ScheduledFuture<?> scheduledFuture;
    private transient volatile Exception flushException;

    @SuppressWarnings("deprecation")
    public DynamicMongoWriter(
            MongoConnectionOptions connectionOptions,
            MongoWriteOptions writeOptions,
            boolean flushOnCheckpoint,
            Sink.InitContext initContext,
            MongoSerializationSchema<IN> serializationSchema) {
        this.connectionOptions = checkNotNull(connectionOptions);
        this.writeOptions = checkNotNull(writeOptions);
        this.serializationSchema = checkNotNull(serializationSchema);
        this.flushOnCheckpoint = flushOnCheckpoint;
        this.batchIntervalMs = writeOptions.getBatchIntervalMs();
        this.batchSize = writeOptions.getBatchSize();

        checkNotNull(initContext);
        this.mailboxExecutor = checkNotNull(initContext.getMailboxExecutor());

        SinkWriterMetricGroup metricGroup = checkNotNull(initContext.metricGroup());
        metricGroup.setCurrentSendTimeGauge(() -> ackTime - lastSendTime);

        this.numRecordsOut = metricGroup.getNumRecordsSendCounter();
        // this.collector = new ListCollector<>(this.bulkRequests);

        // Initialize the serialization schema.
        this.sinkContext = new DefaultMongoSinkContext(initContext, writeOptions);
        try {
            SerializationSchema.InitializationContext initializationContext = initContext
                    .asSerializationSchemaInitializationContext();
            serializationSchema.open(initializationContext, sinkContext, writeOptions);
        } catch (Exception e) {
            throw new FlinkRuntimeException("Failed to open the MongoEmitter", e);
        }

        // Initialize the mongo client.
        this.mongoClient = MongoClients.create(connectionOptions.getUri());

        boolean flushOnlyOnCheckpoint = batchIntervalMs == -1 && batchSize == -1;

        if (!flushOnlyOnCheckpoint && batchIntervalMs > 0) {
            this.scheduler = Executors.newScheduledThreadPool(1, new ExecutorThreadFactory("mongo-writer"));

            this.scheduledFuture = this.scheduler.scheduleWithFixedDelay(
                    () -> {
                        synchronized (DynamicMongoWriter.this) {
                            if (!closed && isOverMaxBatchIntervalLimit()) {
                                try {
                                    doBulkWrite();
                                } catch (Exception e) {
                                    flushException = e;
                                }
                            }
                        }
                    },
                    batchIntervalMs,
                    batchIntervalMs,
                    TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public synchronized void write(MetaDocument<IN> element, Context context)
            throws IOException, InterruptedException {
        checkFlushException();

        // do not allow new bulk writes until all actions are flushed
        while (checkpointInProgress) {
            mailboxExecutor.yield();
        }
        numRecordsOut.inc();
        Collector<WriteModel<BsonDocument>> collector = this.collectors.get(element.getDevice());
        if (collector == null) {
            List<WriteModel<BsonDocument>> bulkRequests = new ArrayList<>();
            this.bulkRequests.put(element.getDevice(), bulkRequests);
            collector = new ListCollector<>(bulkRequests);
            this.collectors.put(element.getDevice(), collector);
        }
        WriteModel<BsonDocument> writeModel = serializationSchema.serialize(element.getDocument(), sinkContext);
        collector.collect(writeModel);

        if (isOverMaxBatchSizeLimit() || isOverMaxBatchIntervalLimit()) {
            doBulkWrite();
        }
    }

    @Override
    public synchronized void flush(boolean endOfInput) throws IOException {
        checkFlushException();

        checkpointInProgress = true;
        while (!bulkRequests.isEmpty() && (flushOnCheckpoint || endOfInput)) {
            doBulkWrite();
        }
        checkpointInProgress = false;
    }

    @Override
    public synchronized void close() throws Exception {
        if (!closed) {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
                scheduler.shutdown();
            }

            if (!bulkRequests.isEmpty()) {
                try {
                    doBulkWrite();
                } catch (Exception e) {
                    LOG.error("Writing records to MongoDB failed when closing MongoWriter", e);
                    throw new IOException("Writing records to MongoDB failed.", e);
                } finally {
                    mongoClient.close();
                    closed = true;
                }
            } else {
                mongoClient.close();
                closed = true;
            }
        }
    }

    @VisibleForTesting
    void doBulkWrite() throws IOException {
        if (bulkRequests.isEmpty()) {
            // no records to write
            return;
        }

        int maxRetries = writeOptions.getMaxRetries();
        long retryIntervalMs = writeOptions.getRetryIntervalMs();
        for (int i = 0; i <= maxRetries; i++) {
            try {
                lastSendTime = System.currentTimeMillis();
                for (Map.Entry<String, List<WriteModel<BsonDocument>>> entry : this.bulkRequests.entrySet()) {
                    if (entry.getValue().isEmpty()) {
                        // no records to write
                        continue;
                    }
                    MongoDatabase mongoDatabase = mongoClient.getDatabase(connectionOptions.getDatabase());
                    MongoCollection<BsonDocument> mongoCollection = mongoDatabase.getCollection(entry.getKey(), BsonDocument.class);
                    if (mongoCollection == null) {
                        TimeSeriesOptions timeSeriesOptions = new TimeSeriesOptions("timestamp")
                                .metaField("deviceId")
                                .granularity(TimeSeriesGranularity.MINUTES);
                        CreateCollectionOptions options = new CreateCollectionOptions().timeSeriesOptions(timeSeriesOptions);
                        mongoDatabase.createCollection(entry.getKey(), options);
                    }

                    mongoClient
                            .getDatabase(connectionOptions.getDatabase())
                            .getCollection(entry.getKey(), BsonDocument.class)
                            .bulkWrite(entry.getValue());
                    entry.getValue().clear();
                }
                ackTime = System.currentTimeMillis();
                break;
            } catch (MongoException e) {
                LOG.debug("Bulk Write to MongoDB failed, retry times = {}", i, e);
                if (i >= maxRetries) {
                    LOG.error("Bulk Write to MongoDB failed", e);
                    throw new IOException(e);
                }
                try {
                    Thread.sleep(retryIntervalMs * (i + 1));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException(
                            "Unable to flush; interrupted while doing another attempt", e);
                }
            }
        }
    }

    private boolean isOverMaxBatchSizeLimit() {
        return batchSize != -1 && bulkRequests.size() >= batchSize;
    }

    private boolean isOverMaxBatchIntervalLimit() {
        long lastSentInterval = System.currentTimeMillis() - lastSendTime;
        return batchIntervalMs != -1 && lastSentInterval >= batchIntervalMs;
    }

    private void checkFlushException() {
        if (flushException != null) {
            throw new RuntimeException("Writing records to MongoDB failed.", flushException);
        }
    }
}
