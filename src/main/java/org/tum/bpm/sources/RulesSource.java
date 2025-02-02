package org.tum.bpm.sources;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.cdc.connectors.base.options.StartupOptions;
import org.apache.flink.cdc.connectors.mongodb.source.MongoDBSource;
import org.apache.flink.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.tum.configuration.MongoConfiguration;

public class RulesSource {

    public static MongoConfiguration mongoConfiguration = MongoConfiguration.getConfiguration();

    public static MongoDBSource<String> createRulesIncrementalSource() throws IOException {
        MongoDBSource<String> mongoSource = MongoDBSource.<String>builder()
                .scheme(mongoConfiguration.getProperty("mongodb.scheme"))
                .hosts(mongoConfiguration.getProperty("mongodb.hosts"))
                .username(mongoConfiguration.getProperty("mongodb.userName"))
                .password(mongoConfiguration.getProperty("mongodb.password"))
                .connectionOptions(mongoConfiguration.getProperty("mongodb.connectionOptions"))
                .startupOptions(StartupOptions.initial())
                .batchSize(8)
                .databaseList("bpm_event_processing") // set captured database, support regex
                .collectionList("bpm_event_processing.event_abstraction_rules",
                        "bpm_event_processing.event_enrichment_rules",
                        "bpm_event_processing.event_scoping_rules",
                        "bpm_event_processing.resource_name_rules")
                .deserializer(new JsonDebeziumDeserializationSchema())
                .build();
        return mongoSource;
    }

    public static WatermarkStrategy<String> createWatermarkStrategy() {
        WatermarkStrategy<String> watermarkStrategy = WatermarkStrategy
                // Handle data that is max 20 seconds out of order
                .<String>forBoundedOutOfOrderness(Duration.ofSeconds(1))
                .withTimestampAssigner((event, timestamp) -> Instant.now().toEpochMilli())
                .withIdleness(Duration.ofSeconds(1));
        return watermarkStrategy;
    }
}
