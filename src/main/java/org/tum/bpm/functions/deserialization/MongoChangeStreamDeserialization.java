package org.tum.bpm.functions.deserialization;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.tum.bpm.schemas.debezium.MongoChangeStreamMessage;

/**
 * Deserializes Change Stream Messages into MongoDb
 */
public class MongoChangeStreamDeserialization extends ProcessFunction<String, MongoChangeStreamMessage> {

    private transient ObjectMapper objectMapper;

    @Override
    public void open(Configuration parameters) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void processElement(String value, ProcessFunction<String, MongoChangeStreamMessage>.Context ctx,
            Collector<MongoChangeStreamMessage> out) throws Exception {
        
        MongoChangeStreamMessage changeMessage = objectMapper.readValue(value, MongoChangeStreamMessage.class);
        out.collect(changeMessage);
    }
    
}
