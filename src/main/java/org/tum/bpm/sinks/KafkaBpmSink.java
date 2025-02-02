package org.tum.bpm.sinks;

import java.io.IOException;

import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.formats.json.JsonSerializationSchema;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.tum.bpm.schemas.ocel.OcelEvent;
import org.tum.bpm.schemas.ocel.OcelObject;
import org.tum.configuration.KafkaConfiguration;

public class KafkaBpmSink {

    private static final String OCEL_EVENT_TOPIC = "eh-bpm-ocelevents-prod";
    private static final String OCEL_OBJECT_TOPIC = "eh-bpm-ocelobjects-prod";
    public static KafkaConfiguration kafkaConfiguration = KafkaConfiguration.getConfiguration();

    public static KafkaSink<OcelEvent> createOcelEventSink() throws IOException {

        KafkaSink<OcelEvent> sink = KafkaSink.<OcelEvent>builder()
                .setBootstrapServers(kafkaConfiguration.getProperty("bootstrap.servers"))
                .setKafkaProducerConfig(kafkaConfiguration.getProperties())
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic(OCEL_EVENT_TOPIC)
                        .setValueSerializationSchema(new JsonSerializationSchema<OcelEvent>(
                                () -> new ObjectMapper()
                                        .registerModule(new JavaTimeModule())))
                        .build())
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();

        return sink;
    }

    public static KafkaSink<OcelObject> createOcelObjectSink() throws IOException {
        KafkaSink<OcelObject> sink = KafkaSink.<OcelObject>builder()
                .setBootstrapServers(kafkaConfiguration.getProperty("bootstrap.servers"))
                .setKafkaProducerConfig(kafkaConfiguration.getProperties())
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic(OCEL_OBJECT_TOPIC)
                        .setValueSerializationSchema(new JsonSerializationSchema<OcelObject>(
                                () -> new ObjectMapper()
                                        .registerModule(new JavaTimeModule())))
                        .build())
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();

        return sink;
    }
}
