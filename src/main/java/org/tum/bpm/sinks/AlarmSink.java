package org.tum.bpm.sinks;

import java.io.IOException;

import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.formats.json.JsonSerializationSchema;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.tum.bpm.schemas.stats.Alarm;
import org.tum.configuration.KafkaConfiguration;

public class AlarmSink {

    private static final String ALARM_TOPIC = "eh-bpm-alarms-prod";
    public static KafkaConfiguration kafkaConfiguration = KafkaConfiguration.getConfiguration();

    public static KafkaSink<Alarm> createAlarmSink() throws IOException {

        KafkaSink<Alarm> sink = KafkaSink.<Alarm>builder()
                .setBootstrapServers(kafkaConfiguration.getProperty("bootstrap.servers"))
                .setKafkaProducerConfig(kafkaConfiguration.getProperties())
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic(ALARM_TOPIC)
                        .setValueSerializationSchema(new JsonSerializationSchema<Alarm>(
                                () -> new ObjectMapper()
                                        .registerModule(new JavaTimeModule())
                                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)))
                        .build())
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();

        return sink;
    }
}
