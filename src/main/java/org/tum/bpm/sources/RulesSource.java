package org.tum.bpm.sources;

import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.cdc.connectors.base.options.StartupOptions;
import org.apache.flink.cdc.connectors.mongodb.source.MongoDBSource;
import org.apache.flink.cdc.debezium.JsonDebeziumDeserializationSchema;

public class RulesSource {

    private static final String MONGO_CONFIG_PATH = "src/main/resources/mongodb.config";

    public static MongoDBSource<String> createRulesIncrementalSource() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileReader(MONGO_CONFIG_PATH));

        MongoDBSource<String> mongoSource = MongoDBSource.<String>builder()
                .scheme(properties.getProperty("mongodb.scheme"))
                .hosts(properties.getProperty("mongodb.hosts"))
                .connectionOptions("replicaSet=myReplicaSet&appName=flink")
                .startupOptions(StartupOptions.initial())
                .batchSize(8)
                .databaseList("bpm_event_processing") // set captured database, support regex
                .collectionList("bpm_event_processing.event_abstraction_rules",
                        "bpm_event_processing.event_enrichment_rules",
                        "bpm_event_processing.event_scoping_rules",
                        "bpm_event_processing.event_resource_correlation_rules") // set captured collections, support regex
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
