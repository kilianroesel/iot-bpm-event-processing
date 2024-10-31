package org.tum.bpm.sinks;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.formats.json.JsonSerializationSchema;
import org.tum.bpm.schemas.CorrelatedEvent;
import org.tum.bpm.schemas.EnrichedEvent;

public class KafkaBpmSink {

    private static final String TOPIC = "eh-bpm-events-prod";
    private static final String FILE_PATH = "src/main/resources/kafka.config";
    
    public static KafkaSink<CorrelatedEvent> createEventSink() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileReader(FILE_PATH));

        KafkaSink<CorrelatedEvent> sink = KafkaSink.<CorrelatedEvent>builder()
            .setBootstrapServers(properties.getProperty("bootstrap.servers"))
            .setKafkaProducerConfig(properties)
            .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                .setTopic(TOPIC)
                .setValueSerializationSchema(new JsonSerializationSchema<CorrelatedEvent>())
                .build()
            )
        .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
        .build();

        return sink;
    }

    public static KafkaSink<EnrichedEvent> createResourceSink() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileReader(FILE_PATH));

        KafkaSink<EnrichedEvent> sink = KafkaSink.<EnrichedEvent>builder()
            .setBootstrapServers(properties.getProperty("bootstrap.servers"))
            .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                .setTopic(TOPIC)
                .setValueSerializationSchema(new JsonSerializationSchema<EnrichedEvent>())
                .build()
            )
        .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
        .build();

        return sink;
    }
}
