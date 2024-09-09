package org.tum.bpm.sources;

import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.json.JsonDeserializationSchema;
import org.tum.bpm.schemas.measurements.IoTMessageSchema;

public class MeasurementKafkaSource {

    private static final String TOPIC = "eh-bpm-event-processing-prod";
    private static final String FILE_PATH = "src/main/resources/kafka.config";

    public static KafkaSource<IoTMessageSchema> createMeasurementSource() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileReader(FILE_PATH));

        KafkaSource<IoTMessageSchema> source = KafkaSource.<IoTMessageSchema>builder()
                .setProperties(properties)
                .setTopics(TOPIC)
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new JsonDeserializationSchema<IoTMessageSchema>(IoTMessageSchema.class))
                .build();

        return source;
    }

    public static WatermarkStrategy<IoTMessageSchema> createWatermarkStrategy() {
        WatermarkStrategy<IoTMessageSchema> watermarkStrategy = WatermarkStrategy
                // Handle data that is max 20 seconds out of order
                .<IoTMessageSchema>forBoundedOutOfOrderness(Duration.ofSeconds(10))
                // Assign event Time Timestamps to each measurement
                .withTimestampAssigner((measurement, timestamp) -> measurement.getPayload().getTimestampUtc().toInstant().toEpochMilli())
                // If a source does not generate events for 60 seconds it is considered idle and the watermark progresses
                .withIdleness(Duration.ofSeconds(10));
        return watermarkStrategy;
    }
}
