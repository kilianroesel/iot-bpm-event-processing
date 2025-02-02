package org.tum.bpm.sinks;

import java.io.IOException;

import org.apache.flink.connector.base.DeliveryGuarantee;
import org.tum.bpm.functions.serialization.OcelEventSerializationMongo;
import org.tum.bpm.schemas.ocel.OcelEvent;
import org.tum.configuration.MongoConfiguration;

import org.apache.flink.connector.mongodb.sink.MongoSink;

public class MongoBpmSink {

    public static MongoConfiguration mongoConfiguration = MongoConfiguration.getConfiguration();

    public static MongoSink<OcelEvent> createOcelEventSink() throws IOException {

        String uri = mongoConfiguration.getProperty("mongodb.scheme") + "://"
                + mongoConfiguration.getProperty("mongodb.userName") + ":"
                + mongoConfiguration.getProperty("mongodb.password") + "@"
                + mongoConfiguration.getProperty("mongodb.hosts");
        MongoSink<OcelEvent> sink = MongoSink.<OcelEvent>builder()
                .setUri(uri)
                .setDatabase("bpm_ocel")
                .setCollection("bpm_ocel_events")
                .setBatchSize(1000)
                .setBatchIntervalMs(1000)
                .setMaxRetries(3)
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .setSerializationSchema(new OcelEventSerializationMongo())
                .build();

        return sink;
    }
}