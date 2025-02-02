package org.tum.bpm.functions.serialization;

import java.util.ArrayList;
import java.util.List;

import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.tum.bpm.schemas.Resource;
import org.tum.bpm.schemas.ocel.OcelObject;
import org.tum.bpm.schemas.ocel.OcelRelationship;
import org.tum.bpm.schemas.rules.ResourceNameRule;
import org.tum.bpm.schemas.rules.RuleControl;

public class OcelObjectSerialization
        extends BroadcastProcessFunction<Resource, RuleControl<ResourceNameRule>, OcelObject> {

    // Broadcast state
    public static final MapStateDescriptor<String, ResourceNameRule> RESOURCE_NAME_RULE_BROADCAST_STATE_DESCRIPTOR = new MapStateDescriptor<String, ResourceNameRule>(
            "resourceNameRulesBroadcastState",
            BasicTypeInfo.STRING_TYPE_INFO,
            TypeInformation.of(new TypeHint<ResourceNameRule>() {
            }));

    @Override
    public void processBroadcastElement(RuleControl<ResourceNameRule> ruleControl,
            BroadcastProcessFunction<Resource, RuleControl<ResourceNameRule>, OcelObject>.Context ctx,
            Collector<OcelObject> out) throws Exception {

        BroadcastState<String, ResourceNameRule> broadcastState = ctx
                .getBroadcastState(RESOURCE_NAME_RULE_BROADCAST_STATE_DESCRIPTOR);

        // Update broadcast rule state depending on rule control
        if (ruleControl.getControl() == RuleControl.Control.ACTIVE) {
            broadcastState.put(ruleControl.getRule().getRuleId(), ruleControl.getRule());
        } else if (ruleControl.getControl().equals(RuleControl.Control.INACTIVE)) {
            broadcastState.remove(ruleControl.getRule().getRuleId());
        }
    }

    @Override
    public void processElement(Resource resource,
            BroadcastProcessFunction<Resource, RuleControl<ResourceNameRule>, OcelObject>.ReadOnlyContext ctx,
            Collector<OcelObject> out) throws Exception {

        ReadOnlyBroadcastState<String, ResourceNameRule> rulesState = ctx
                .getBroadcastState(RESOURCE_NAME_RULE_BROADCAST_STATE_DESCRIPTOR);

        // These created resource do not have any relation amongst each other at the moment
        List<OcelRelationship> ocelRelationships = new ArrayList<>();

        String typeId = resource.getResourceModelId();
        ResourceNameRule rule = rulesState.get(typeId);
        if (rule == null) {
            return;
        } else {
            OcelObject ocelObject = new OcelObject(resource.getResourceId(), rule.getResourceModelName(), resource.getEnrichment(), ocelRelationships);
            out.collect(ocelObject);
        }
    }
}
