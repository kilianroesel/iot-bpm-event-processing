package org.tum.bpm.sinks.dynamicMongoSink;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.mongodb.common.config.MongoConnectionOptions;
import org.apache.flink.connector.mongodb.sink.MongoSink;
import org.apache.flink.connector.mongodb.sink.config.MongoWriteOptions;
import org.apache.flink.connector.mongodb.sink.writer.serializer.MongoSerializationSchema;
import org.apache.flink.util.InstantiationUtil;

import com.mongodb.client.model.WriteModel;

import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.Preconditions.checkState;

/**
 * Base builder to construct a {@link MongoSink}.
 *
 * @param <IN> type of the records converted to MongoDB bulk request
 */
@PublicEvolving
public class DynamicMongoSinkBuilder<IN> {

    private final MongoConnectionOptions.MongoConnectionOptionsBuilder connectionOptionsBuilder;
    private final MongoWriteOptions.MongoWriteOptionsBuilder writeOptionsBuilder;

    private MongoSerializationSchema<IN> serializationSchema;

    DynamicMongoSinkBuilder() {
        this.connectionOptionsBuilder = MongoConnectionOptions.builder();
        this.writeOptionsBuilder = MongoWriteOptions.builder();
    }

    /**
     * Sets the connection string of MongoDB.
     *
     * @param uri connection string of MongoDB
     * @return this builder
     */
    public DynamicMongoSinkBuilder<IN> setUri(String uri) {
        connectionOptionsBuilder.setUri(uri);
        return this;
    }

    /**
     * Sets the database to sink of MongoDB.
     *
     * @param database the database to sink of MongoDB.
     * @return this builder
     */
    public DynamicMongoSinkBuilder<IN> setDatabase(String database) {
        connectionOptionsBuilder.setDatabase(database);
        return this;
    }

    /**
     * Sets the maximum number of actions to buffer for each batch request. You can
     * pass -1 to
     * disable batching.
     *
     * @param batchSize the maximum number of actions to buffer for each batch
     *                  request.
     * @return this builder
     */
    public DynamicMongoSinkBuilder<IN> setBatchSize(int batchSize) {
        writeOptionsBuilder.setBatchSize(batchSize);
        return this;
    }

    /**
     * Sets the batch flush interval, in milliseconds. You can pass -1 to disable
     * it.
     *
     * @param batchIntervalMs the batch flush interval, in milliseconds.
     * @return this builder
     */
    public DynamicMongoSinkBuilder<IN> setBatchIntervalMs(long batchIntervalMs) {
        writeOptionsBuilder.setBatchIntervalMs(batchIntervalMs);
        return this;
    }

    /**
     * Sets the max retry times if writing records failed.
     *
     * @param maxRetries the max retry times.
     * @return this builder
     */
    public DynamicMongoSinkBuilder<IN> setMaxRetries(int maxRetries) {
        writeOptionsBuilder.setMaxRetries(maxRetries);
        return this;
    }

    /**
     * Sets the wanted {@link DeliveryGuarantee}. The default delivery guarantee is
     * {@link
     * DeliveryGuarantee#AT_LEAST_ONCE}
     *
     * @param deliveryGuarantee which describes the record emission behaviour
     * @return this builder
     */
    public DynamicMongoSinkBuilder<IN> setDeliveryGuarantee(DeliveryGuarantee deliveryGuarantee) {
        writeOptionsBuilder.setDeliveryGuarantee(deliveryGuarantee);
        return this;
    }

    /**
     * Sets the collection to sink of MongoDB.
     *
     * @param collection the collection to sink of MongoDB.
     * @return this builder
     */
    public DynamicMongoSinkBuilder<IN> setCollection(String collection) {
        connectionOptionsBuilder.setCollection(collection);
        return this;
    }

    /**
     * Sets the serialization schema which is invoked on every record to convert it
     * to MongoDB bulk
     * request.
     *
     * @param serializationSchema to process records into MongoDB bulk
     *                            {@link WriteModel}.
     * @return this builder
     */
    public DynamicMongoSinkBuilder<IN> setSerializationSchema(
            MongoSerializationSchema<IN> serializationSchema) {
        checkNotNull(serializationSchema);
        checkState(
                InstantiationUtil.isSerializable(serializationSchema),
                "The mongo serialization schema must be serializable.");
        this.serializationSchema = serializationSchema;
        return this;
    }

    /**
     * Constructs the {@link MongoSink} with the properties configured this builder.
     *
     * @return {@link MongoSink}
     */
    public DynamicMongoSink<IN> build() {
        checkNotNull(serializationSchema, "The serialization schema must be supplied");
        return new DynamicMongoSink<>(
                connectionOptionsBuilder.build(), writeOptionsBuilder.build(), serializationSchema);
    }
}
