package org.tum.bpm.functions.serialization;

import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.connector.mongodb.sink.config.MongoWriteOptions;
import org.apache.flink.connector.mongodb.sink.writer.context.MongoSinkContext;
import org.apache.flink.connector.mongodb.sink.writer.serializer.MongoSerializationSchema;
import org.bson.BsonDocument;
import org.tum.bpm.schemas.ocel.OcelEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;

public class OcelEventSerializationMongo implements MongoSerializationSchema<OcelEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void open(
            SerializationSchema.InitializationContext initializationContext,
            MongoSinkContext sinkContext,
            MongoWriteOptions sinkConfiguration)
            throws Exception {
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public WriteModel<BsonDocument> serialize(OcelEvent element, MongoSinkContext sinkContext) {
        try {
            String json = objectMapper.writeValueAsString(element);
            return new InsertOneModel<>(BsonDocument.parse(json));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing object to JSON", e);
        }
    }
}