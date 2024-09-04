package org.tum.bpm.functions.serialization;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.tum.bpm.schemas.debezium.DebeziumMessage;
import org.tum.bpm.schemas.rules.EventAbstractionRule;
import org.tum.bpm.schemas.rules.EventEnrichmentRule;
import org.tum.bpm.schemas.rules.EventScopingRule;

/**
 * This function deserializes Json String Debezium Messages into POJO Java objects.
 * It seperates them into different output tags depedning on their payload class.
 */
public class DebeziumDeserializationFunction extends ProcessFunction<String, DebeziumMessage<?>> {
    private transient ObjectMapper objectMapper;

    public static final OutputTag<DebeziumMessage<EventAbstractionRule>> RAW_EVENT_ABSTRACTION_RULE_OUTPUT_TAG = new OutputTag<DebeziumMessage<EventAbstractionRule>>(
            "rawEventAbstractionRuleOutputTag") {
    };

    public static final OutputTag<DebeziumMessage<EventScopingRule>> RAW_EVENT_SCOPING_RULE_OUTPUT_TAG = new OutputTag<DebeziumMessage<EventScopingRule>>(
            "rawEventScopingRuleOutputTag") {
    };

    public static final OutputTag<DebeziumMessage<EventEnrichmentRule>> RAW_EVENT_ENRICHMENT_RULE_OUTPUT_TAG = new OutputTag<DebeziumMessage<EventEnrichmentRule>>(
        "rawEventEnrichmentRuleOutputTag") {
};

    @Override
    public void open(Configuration parameters) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void processElement(String value, ProcessFunction<String, DebeziumMessage<?>>.Context ctx,
            Collector<DebeziumMessage<?>> out) throws Exception {
        DebeziumMessage<?> dMessage = objectMapper.readValue(value, DebeziumMessage.class);
        String table = dMessage.getSource().getTable();

        if (table.equals("EventScopingRule")) {
            DebeziumMessage<EventScopingRule> machineDescription = objectMapper.readValue(value,
                    new TypeReference<DebeziumMessage<EventScopingRule>>() {
                    });
            ctx.output(RAW_EVENT_SCOPING_RULE_OUTPUT_TAG, machineDescription);

        } else if (table.equals("EventAbstractionRule")) {
            DebeziumMessage<EventAbstractionRule> eventDescription = objectMapper.readValue(value,
                    new TypeReference<DebeziumMessage<EventAbstractionRule>>() {
                    });
            ctx.output(RAW_EVENT_ABSTRACTION_RULE_OUTPUT_TAG, eventDescription);

        } else if (table.equals("EventEnrichmentRule")) {
            DebeziumMessage<EventEnrichmentRule> eventDescription = objectMapper.readValue(value,
                    new TypeReference<DebeziumMessage<EventEnrichmentRule>>() {
                    });
            ctx.output(RAW_EVENT_ENRICHMENT_RULE_OUTPUT_TAG, eventDescription);
        }
        out.collect(dMessage);
    }
}
