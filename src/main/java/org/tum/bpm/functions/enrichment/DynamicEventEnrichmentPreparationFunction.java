package org.tum.bpm.functions.enrichment;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.tum.bpm.schemas.AbstractedEvent;
import org.tum.bpm.schemas.EquipmentListEvent;
import org.tum.bpm.schemas.rules.EventEnrichmentRule;
import org.tum.bpm.schemas.rules.RuleControl;

public class DynamicEventEnrichmentPreparationFunction extends BroadcastProcessFunction<AbstractedEvent, RuleControl<EventEnrichmentRule>, EquipmentListEvent> {

    // Broadcast state
    public static final MapStateDescriptor<String, List<EventEnrichmentRule>> ENRICHMENT_RULES_BROADCAST_STATE_DESCRIPTOR = new MapStateDescriptor<String, List<EventEnrichmentRule>>(
            "enrichmentRulesBroadcastState",
            BasicTypeInfo.STRING_TYPE_INFO,
            TypeInformation.of(new TypeHint<List<EventEnrichmentRule>>() {
            }));

    @Override
    public void processElement(AbstractedEvent baseEvent,
            BroadcastProcessFunction<AbstractedEvent, RuleControl<EventEnrichmentRule>, EquipmentListEvent>.ReadOnlyContext ctx,
            Collector<EquipmentListEvent> out) throws Exception {
        ReadOnlyBroadcastState<String, List<EventEnrichmentRule>> enrichmentRuleState = ctx
                .getBroadcastState(ENRICHMENT_RULES_BROADCAST_STATE_DESCRIPTOR);

        List<EventEnrichmentRule> enrichmentRules = enrichmentRuleState.get(baseEvent.getRule().getViewId());
        EquipmentListEvent equipmentListEvent = new EquipmentListEvent(baseEvent, enrichmentRules);

        out.collect(equipmentListEvent);
    }

    @Override
    public void processBroadcastElement(RuleControl<EventEnrichmentRule> ruleControl,
            BroadcastProcessFunction<AbstractedEvent, RuleControl<EventEnrichmentRule>, EquipmentListEvent>.Context ctx,
            Collector<EquipmentListEvent> out) throws Exception {

        EventEnrichmentRule rule = ruleControl.getRule();
        BroadcastState<String, List<EventEnrichmentRule>> broadcastState = ctx
                .getBroadcastState(ENRICHMENT_RULES_BROADCAST_STATE_DESCRIPTOR);

        switch (ruleControl.getControl()) {
            case ACTIVE:
                List<EventEnrichmentRule> rules = broadcastState.get(rule.getViewId());
                if (rules == null) {
                    rules = new ArrayList<>();
                }
                // Removes an old rule by the field
                rules.removeIf(currentRule -> currentRule.getRuleId().equals(rule.getRuleId()));
                rules.add(rule);
                broadcastState.put(rule.getViewId(), rules);
                break;
            case INACTIVE:
                // Removes an inactive rule by id, we need to do that, because we only know of
                // the id on delete
                for (Map.Entry<String, List<EventEnrichmentRule>> fieldRuleMap : broadcastState.entries()) {
                    String key = fieldRuleMap.getKey();
                    rules = fieldRuleMap.getValue();
                    rules.removeIf(currentRule -> currentRule.getRuleId().equals(rule.getRuleId()));
                    broadcastState.put(key, rules);
                }
                break;
            default:
                break;
        }
    }
}
