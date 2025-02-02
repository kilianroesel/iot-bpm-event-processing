package org.tum.bpm.functions.deserialization;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.jsontype.NamedType;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.tum.bpm.schemas.debezium.MongoChangeStreamMessage;
import org.tum.bpm.schemas.rules.EventAbstractionRule;
import org.tum.bpm.schemas.rules.EventEnrichmentRule;
import org.tum.bpm.schemas.rules.EventScopingRule;
import org.tum.bpm.schemas.rules.ResourceNameRule;
import org.tum.bpm.schemas.rules.Rule;
import org.tum.bpm.schemas.rules.RuleControl;

public class RulesDeserialization extends ProcessFunction<MongoChangeStreamMessage, RuleControl<Rule>> {

    private transient ObjectMapper objectMapper;

    public static final OutputTag<RuleControl<EventAbstractionRule>> EVENT_ABSTRACTION_RULE_OUTPUT_TAG = new OutputTag<RuleControl<EventAbstractionRule>>(
            "eventAbstractionRuleOutputTag") {
    };

    public static final OutputTag<RuleControl<EventScopingRule>> EVENT_SCOPING_RULE_OUTPUT_TAG = new OutputTag<RuleControl<EventScopingRule>>(
            "eventScopingRuleOutputTag") {
    };

    public static final OutputTag<RuleControl<EventEnrichmentRule>> EVENT_ENRICHMENT_RULE_OUTPUT_TAG = new OutputTag<RuleControl<EventEnrichmentRule>>(
            "eventEnrichmentRuleOutputTag") {
    };

    public static final OutputTag<RuleControl<ResourceNameRule>> RESOURCE_NAME_RULE_OUTPUT_TAG = new OutputTag<RuleControl<ResourceNameRule>>(
            "eventResourceNameRules") {
    };

    @Override
    public void open(Configuration parameters) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.registerSubtypes(new NamedType(EventAbstractionRule.class, "EventAbstractionRule"));
        this.objectMapper.registerSubtypes(new NamedType(EventScopingRule.class, "EventScopingRule"));
        this.objectMapper.registerSubtypes(new NamedType(EventEnrichmentRule.class, "EventEnrichmentRule"));
        this.objectMapper.registerSubtypes(new NamedType(ResourceNameRule.class, "ResourceNameRule"));
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void processElement(MongoChangeStreamMessage changeStreamMessage,
            ProcessFunction<MongoChangeStreamMessage, RuleControl<Rule>>.Context ctx, Collector<RuleControl<Rule>> out)
            throws Exception {

        Rule rule = null;
        RuleControl.Control control = null;
        if (changeStreamMessage.getOperationType().equals("insert")
                || changeStreamMessage.getOperationType().equals("update")) {
            JsonNode jsonNode = objectMapper.readTree(changeStreamMessage.getFullDocument());
            ((ObjectNode) jsonNode).put("id", jsonNode.get("_id").get("$oid").asText());
            ((ObjectNode) jsonNode).put("updatedAt", jsonNode.get("updatedAt").get("$date").asText());
            ((ObjectNode) jsonNode).put("createdAt", jsonNode.get("createdAt").get("$date").asText());

            rule = objectMapper.treeToValue(jsonNode, Rule.class);
            control = RuleControl.Control.ACTIVE;
        }
        if (changeStreamMessage.getOperationType().equals("delete")) {
            JsonNode jsonNode = objectMapper.readTree(changeStreamMessage.getId());
            String ruleId = jsonNode.get("_id").get("_id").get("$oid").asText();
            String ruleType = changeStreamMessage.getNamespace().getCollection();

            rule = new Rule();
            if (ruleType.equals("event_enrichment_rules")) {
                rule = new EventEnrichmentRule();
                rule.setRuleId(ruleId);
            }
            if (ruleType.equals("event_abstraction_rules")) {
                rule = new EventAbstractionRule();
                rule.setRuleId(ruleId);
            }
            if (ruleType.equals("event_scoping_rules")) {
                rule = new EventScopingRule();
                rule.setRuleId(ruleId);
            }
            if (ruleType.equals("resource_name_rules")) {
                rule = new ResourceNameRule();
                rule.setRuleId(ruleId);
            }
            control = RuleControl.Control.INACTIVE;
        }
        if (rule == null || control == null) {
            return;
        }

        if (rule instanceof EventScopingRule) {
            RuleControl<EventScopingRule> ruleControl = new RuleControl<EventScopingRule>((EventScopingRule) rule,
                    control);
            ctx.output(EVENT_SCOPING_RULE_OUTPUT_TAG, ruleControl);
        } else if (rule instanceof EventAbstractionRule) {
            RuleControl<EventAbstractionRule> ruleControl = new RuleControl<EventAbstractionRule>(
                    (EventAbstractionRule) rule, control);
            ctx.output(EVENT_ABSTRACTION_RULE_OUTPUT_TAG, ruleControl);
        } else if (rule instanceof EventEnrichmentRule) {
            RuleControl<EventEnrichmentRule> ruleControl = new RuleControl<EventEnrichmentRule>(
                    (EventEnrichmentRule) rule, control);
            ctx.output(EVENT_ENRICHMENT_RULE_OUTPUT_TAG, ruleControl);
        } else if (rule instanceof ResourceNameRule) {
            RuleControl<ResourceNameRule> ruleControl = new RuleControl<ResourceNameRule>((ResourceNameRule) rule,
            control);
            ctx.output(RESOURCE_NAME_RULE_OUTPUT_TAG, ruleControl);
        }
    }
}
