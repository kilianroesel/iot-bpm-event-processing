package org.tum.bpm.sinks;

import java.io.IOException;

import org.apache.flink.connector.base.DeliveryGuarantee;
import org.tum.bpm.functions.serialization.OcelEventSerializationMongo;
import org.tum.bpm.schemas.ocel.OcelEvent;
import org.tum.bpm.sinks.dynamicMongoSink.DynamicMongoSink;
import org.tum.configuration.MongoConfiguration;
public class MongoBpmSink {

    public static MongoConfiguration mongoConfiguration = MongoConfiguration.getConfiguration();

    public static DynamicMongoSink<OcelEvent> createOcelEventSink() throws IOException {
        String uri = mongoConfiguration.getProperty("mongodb.scheme") + "://"
                + mongoConfiguration.getProperty("mongodb.userName") + ":"
                + mongoConfiguration.getProperty("mongodb.password") + "@"
                + mongoConfiguration.getProperty("mongodb.hosts");
        DynamicMongoSink<OcelEvent> sink = DynamicMongoSink.<OcelEvent>builder()
                .setUri(uri)
                .setDatabase("bpm_ocel")
                .setCollection("test") // Collection must not be null, but is not used by DynamicMongoSink
                .setBatchSize(1000)
                .setBatchIntervalMs(1000)
                .setMaxRetries(3)
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .setSerializationSchema(new OcelEventSerializationMongo())
                .build();

        return sink;
    }
}