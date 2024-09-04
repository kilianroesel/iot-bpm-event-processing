package org.tum.bpm.sources;

import java.io.IOException;
import java.io.FileReader;
import java.util.Properties;
import java.time.Duration;
import java.time.Instant;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.cdc.connectors.base.source.jdbc.JdbcIncrementalSource;
import org.apache.flink.cdc.connectors.postgres.source.PostgresSourceBuilder;
import org.apache.flink.cdc.debezium.DebeziumDeserializationSchema;
import org.apache.flink.cdc.debezium.JsonDebeziumDeserializationSchema;

public class RulesSource {

    private static final String FILE_PATH = "src/main/resources/rules.config";

    public static JdbcIncrementalSource<String> creatRulesSource() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileReader(FILE_PATH));

        DebeziumDeserializationSchema<String> deserializer = new JsonDebeziumDeserializationSchema();

        // I have no idea why setting debeziumProperties does not work
        JdbcIncrementalSource<String> postgresIncrementalSource = PostgresSourceBuilder.PostgresIncrementalSource
                .<String>builder()
                .hostname(properties.getProperty("database.hostname"))
                .port(Integer.parseInt(properties.getProperty("database.port")))
                .database(properties.getProperty("database.dbname"))
                .schemaList("public")
                .tableList("public.EventAbstractionRule", "public.EventEnrichmentRule", "public.EventScopingRule")
                .username(properties.getProperty("database.user"))
                .password(properties.getProperty("database.password"))
                .slotName("flink")
                .decodingPluginName("pgoutput")
                .deserializer(deserializer)
                .splitSize(8)
                .build();
        return postgresIncrementalSource;
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
