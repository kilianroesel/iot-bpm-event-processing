package org.tum.bpm.sinks.dynamicMongoSink;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.api.java.ClosureCleaner;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.mongodb.common.config.MongoConnectionOptions;
import org.apache.flink.connector.mongodb.sink.config.MongoWriteOptions;
import org.apache.flink.connector.mongodb.sink.writer.serializer.MongoSerializationSchema;

import com.mongodb.client.model.WriteModel;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * Mongo sink converts each incoming element into MongoDB {@link WriteModel}
 * (bulk write action) and
 * bulk writes to mongodb when the number of actions is greater than batchSize
 * or the flush interval
 * is greater than batchIntervalMs.
 *
 * <p>
 * The following example shows how to create a MongoSink sending records of
 * {@code Document}
 * type.
 *
 * <pre>{@code
 * MongoSink<Document> sink = MongoSink.<Document>builder()
 *         .setUri("mongodb://user:password@127.0.0.1:27017")
 *         .setDatabase("db")
 *         .setBatchSize(5)
 *         .setSerializationSchema(
 *                 (doc, context) -> new InsertOneModel<>(doc.toBsonDocument()))
 *         .build();
 * }</pre>
 *
 * @param <IN> Type of the elements handled by this sink
 */
@PublicEvolving
public class DynamicMongoSink<IN> implements Sink<MetaDocument<IN>> {

    private static final long serialVersionUID = 1L;

    private final MongoConnectionOptions connectionOptions;
    private final MongoWriteOptions writeOptions;
    private final MongoSerializationSchema<IN> serializationSchema;

    DynamicMongoSink(
            MongoConnectionOptions connectionOptions,
            MongoWriteOptions writeOptions,
            MongoSerializationSchema<IN> serializationSchema) {
        this.connectionOptions = checkNotNull(connectionOptions);
        this.writeOptions = checkNotNull(writeOptions);
        this.serializationSchema = checkNotNull(serializationSchema);
        ClosureCleaner.clean(
                serializationSchema, ExecutionConfig.ClosureCleanerLevel.RECURSIVE, true);
    }

    public static <IN> DynamicMongoSinkBuilder<IN> builder() {
        return new DynamicMongoSinkBuilder<IN>();
    }

    @Override
    public SinkWriter<MetaDocument<IN>> createWriter(@SuppressWarnings("deprecation") InitContext context) {
        return new DynamicMongoWriter<>(
                connectionOptions,
                writeOptions,
                writeOptions.getDeliveryGuarantee() == DeliveryGuarantee.AT_LEAST_ONCE,
                context,
                serializationSchema);
    }
}
