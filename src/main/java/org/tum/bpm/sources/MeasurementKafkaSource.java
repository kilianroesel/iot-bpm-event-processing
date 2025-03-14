package org.tum.bpm.sources;

import java.io.IOException;
import java.time.Duration;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.json.JsonDeserializationSchema;
import org.tum.bpm.schemas.measurements.IoTMessageSchema;
import org.tum.configuration.KafkaConfiguration;

public class MeasurementKafkaSource {

    public static KafkaConfiguration kafkaConfiguration = KafkaConfiguration.getConfiguration();

    public static KafkaSource<IoTMessageSchema> createMeasurementSource() throws IOException {
        KafkaSource<IoTMessageSchema> source = KafkaSource.<IoTMessageSchema>builder()
                .setProperties(kafkaConfiguration.getProperties())
                .setTopics("eh-bpm-event-processing-prod")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new JsonDeserializationSchema<IoTMessageSchema>(IoTMessageSchema.class))
                .build();

        return source;
    }

    public static WatermarkStrategy<IoTMessageSchema> createWatermarkStrategy() {
        WatermarkStrategy<IoTMessageSchema> watermarkStrategy = WatermarkStrategy
                // Handle data that is max 20 seconds out of order
                .<IoTMessageSchema>forBoundedOutOfOrderness(Duration.ofMillis(100))
                // Assign event Time Timestamps to each measurement
                .withTimestampAssigner((measurement, timestamp) -> measurement.getPayload().getTimestampUtc().toEpochMilli())
                // If a source does not generate events for 60 seconds it is considered idle and the watermark progresses
                .withIdleness(Duration.ofMillis(500));
        return watermarkStrategy;
    }
}
