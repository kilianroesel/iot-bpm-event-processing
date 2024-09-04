package org.tum.bpm.functions.enrichment;

import java.util.List;
import java.util.ArrayList;

import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.tum.bpm.schemas.BaseEvent;
import org.tum.bpm.schemas.EquipmentListEvent;
import org.tum.bpm.schemas.measurements.IoTMessageSchema;
import org.tum.bpm.schemas.rules.EventEnrichmentRule;
import org.tum.bpm.schemas.rules.RuleControl;

public class DynamicEventEnrichmentPreparationFunction extends
        KeyedBroadcastProcessFunction<String, BaseEvent<IoTMessageSchema>, RuleControl<EventEnrichmentRule>, EquipmentListEvent<IoTMessageSchema>> {

    // Broadcast state
    public static final MapStateDescriptor<String, List<String>> ENRICHMENT_RULES_BROADCAST_STATE_DESCRIPTOR = new MapStateDescriptor<String, List<String>>(
            "enrichmentRulesBroadcastState",
            BasicTypeInfo.STRING_TYPE_INFO,
            TypeInformation.of(new TypeHint<List<String>>() {
            }));

    @Override
    public void processElement(BaseEvent<IoTMessageSchema> baseEvent,
            KeyedBroadcastProcessFunction<String, BaseEvent<IoTMessageSchema>, RuleControl<EventEnrichmentRule>, EquipmentListEvent<IoTMessageSchema>>.ReadOnlyContext ctx,
            Collector<EquipmentListEvent<IoTMessageSchema>> out) throws Exception {
        ReadOnlyBroadcastState<String, List<String>> enrichmentRuleState = ctx
                .getBroadcastState(ENRICHMENT_RULES_BROADCAST_STATE_DESCRIPTOR);
                
        List<String> enrichmentRules = enrichmentRuleState.get(baseEvent.getRule().getEquipmentId());
        EquipmentListEvent<IoTMessageSchema> equipmentListEvent = new EquipmentListEvent<>(baseEvent, enrichmentRules);

        out.collect(equipmentListEvent);
    }

    @Override
    public void processBroadcastElement(RuleControl<EventEnrichmentRule> controlRule,
            KeyedBroadcastProcessFunction<String, BaseEvent<IoTMessageSchema>, RuleControl<EventEnrichmentRule>, EquipmentListEvent<IoTMessageSchema>>.Context ctx,
            Collector<EquipmentListEvent<IoTMessageSchema>> out) throws Exception {

        BroadcastState<String, List<String>> broadcastState = ctx
                .getBroadcastState(ENRICHMENT_RULES_BROADCAST_STATE_DESCRIPTOR);
        List<String> rules = broadcastState.get(controlRule.getRule().getEquipmentId());

        if (rules == null) {
            rules = new ArrayList<>();
        }

        switch (controlRule.getControl()) {
            case ACTIVE:
                rules.removeIf(currentRule -> currentRule.equals(controlRule.getRule().getField()));
                rules.add(controlRule.getRule().getField());
                break;
            case INACTIVE:
                rules.removeIf(currentRule -> currentRule.equals(controlRule.getRule().getField()));
                break;
            default:
                break;
        }
        broadcastState.put(controlRule.getRule().getEquipmentId(), rules);
    }
}
