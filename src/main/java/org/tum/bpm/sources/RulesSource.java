package org.tum.bpm.sources;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.io.FileReader;
import java.util.Properties;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.mongodb.source.MongoSource;
import org.apache.flink.connector.mongodb.source.enumerator.splitter.PartitionStrategy;
import org.apache.flink.connector.mongodb.source.reader.deserializer.MongoDeserializationSchema;
import org.apache.flink.configuration.MemorySize;
import org.bson.BsonDocument;
public class RulesSource {

    private static final String KAFKA_CONFIG_PATH = "src/main/resources/kafka.config";
    private static final String MONGO_CONFIG_PATH = "src/main/resources/mongodb.config";
    private static final String TOPIC = "eh-bpm-rules-prod";

    public static KafkaSource<String> createRulesUpdateSource() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileReader(KAFKA_CONFIG_PATH));

        KafkaSource<String> source = KafkaSource.<String>builder()
                .setProperties(properties)
                .setTopics(TOPIC)
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        return source;
    }

    public static MongoSource<String> createRulesBootstrapSource() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileReader(MONGO_CONFIG_PATH));

        MongoSource<String> mongoSource = MongoSource.<String>builder()
                .setUri(properties.getProperty("mongodb.uri"))
                .setDatabase("bpm_event_processing")
                .setCollection("rules")
                .setFetchSize(2048)
                .setLimit(10000)
                .setNoCursorTimeout(true)
                .setPartitionStrategy(PartitionStrategy.SAMPLE)
                .setPartitionSize(MemorySize.ofMebiBytes(64))
                .setSamplesPerPartition(10)
                .setDeserializationSchema(new MongoDeserializationSchema<String>() {
                    @Override
                    public String deserialize(BsonDocument document) {
                        return document.toJson();
                    }

                    @Override
                    public TypeInformation<String> getProducedType() {
                        return BasicTypeInfo.STRING_TYPE_INFO;
                    }
                })
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
