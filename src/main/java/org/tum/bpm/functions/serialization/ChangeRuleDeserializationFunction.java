package org.tum.bpm.functions.serialization;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.jsontype.NamedType;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.tum.bpm.schemas.rules.EventAbstractionRule;
import org.tum.bpm.schemas.rules.EventEnrichmentRule;
import org.tum.bpm.schemas.rules.EventScopingRule;
import org.tum.bpm.schemas.rules.Rule;

public class ChangeRuleDeserializationFunction extends ProcessFunction<String, Rule> {
    private transient ObjectMapper objectMapper;

    public static final OutputTag<EventAbstractionRule> EVENT_ABSTRACTION_RULE_OUTPUT_TAG = new OutputTag<EventAbstractionRule>(
            "eventAbstractionRuleOutputTag") {
    };

    public static final OutputTag<EventScopingRule> EVENT_SCOPING_RULE_OUTPUT_TAG = new OutputTag<EventScopingRule>(
            "eventScopingRuleOutputTag") {
    };

    public static final OutputTag<EventEnrichmentRule> EVENT_ENRICHMENT_RULE_OUTPUT_TAG = new OutputTag<EventEnrichmentRule>(
            "eventEnrichmentRuleOutputTag") {
    };

    @Override
    public void open(Configuration parameters) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.registerSubtypes(new NamedType(EventAbstractionRule.class, "EventAbstractionRule"));
        this.objectMapper.registerSubtypes(new NamedType(EventScopingRule.class, "EventScopingRule"));
        this.objectMapper.registerSubtypes(new NamedType(EventEnrichmentRule.class, "EventEnrichmentRule"));
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void processElement(String value, ProcessFunction<String, Rule>.Context ctx,
            Collector<Rule> out) throws Exception {
        Rule rule = objectMapper.readValue(value, Rule.class);

        if (rule instanceof EventScopingRule) {
            ctx.output(EVENT_SCOPING_RULE_OUTPUT_TAG, (EventScopingRule) rule);
        } else if (rule instanceof EventAbstractionRule) {
            ctx.output(EVENT_ABSTRACTION_RULE_OUTPUT_TAG, (EventAbstractionRule) rule);
        } else if (rule instanceof EventEnrichmentRule) {
            ctx.output(EVENT_ENRICHMENT_RULE_OUTPUT_TAG, (EventEnrichmentRule) rule);
        }
        out.collect(rule);
    }
}
