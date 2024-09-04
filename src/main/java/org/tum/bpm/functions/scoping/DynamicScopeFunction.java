package org.tum.bpm.functions.scoping;

import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.tum.bpm.schemas.Scoped;
import org.tum.bpm.schemas.measurements.IoTMessageSchema;
import org.tum.bpm.schemas.rules.EventScopingRule;
import org.tum.bpm.schemas.rules.RuleControl;
import org.tum.bpm.schemas.rules.RuleControl.Control;

import java.util.Map;

public class DynamicScopeFunction
        extends BroadcastProcessFunction<IoTMessageSchema, RuleControl<EventScopingRule>, Scoped<IoTMessageSchema, String>> {

    // Broadcast state
    public static final MapStateDescriptor<String, EventScopingRule> SCOPE_RULES_BROADCAST_STATE_DESCRIPTOR = new MapStateDescriptor<String, EventScopingRule>(
            "scopeRulesBroadcastState",
            BasicTypeInfo.STRING_TYPE_INFO,
            TypeInformation.of(new TypeHint<EventScopingRule>() {
            }));

    @Override
    public void processElement(IoTMessageSchema measurement,
            BroadcastProcessFunction<IoTMessageSchema, RuleControl<EventScopingRule>, Scoped<IoTMessageSchema, String>>.ReadOnlyContext ctx,
            Collector<Scoped<IoTMessageSchema, String>> out) throws Exception {

        ReadOnlyBroadcastState<String, EventScopingRule> rulesState = ctx
                .getBroadcastState(SCOPE_RULES_BROADCAST_STATE_DESCRIPTOR);

        String scope = measurement.getPayload().getMachineName()
                + measurement.getPayload().getVersionCsiStd()
                + measurement.getPayload().getVersionCsiSpecific()
                + measurement.getPayload().getMachineSoftwareVersion()
                + measurement.getPayload().getMachineMasterSoftwareVersion();

        EventScopingRule rule = rulesState.get(scope);
        if (rule == null) {
            // TODO put ignored measurements into side-output
            return;
        }

        out.collect(new Scoped<IoTMessageSchema, String>(measurement, rule.getId()));
    }

    @Override
    public void processBroadcastElement(RuleControl<EventScopingRule> controlRule,
            BroadcastProcessFunction<IoTMessageSchema, RuleControl<EventScopingRule>, Scoped<IoTMessageSchema, String>>.Context ctx,
            Collector<Scoped<IoTMessageSchema, String>> out) throws Exception {

        BroadcastState<String, EventScopingRule> broadcastState = ctx
                .getBroadcastState(SCOPE_RULES_BROADCAST_STATE_DESCRIPTOR);

        // Retrieve current rule key with given scopeId
        String ruleKey = null;
        for (Map.Entry<String, EventScopingRule> entry : broadcastState.entries()) {
            if (entry.getValue().getId().equals(controlRule.getRule().getId())) {
                ruleKey = entry.getKey();
            }
        }
        if (controlRule.getControl().equals(Control.ACTIVE)) {
            String scope = controlRule.getRule().getMachineName()
                    + controlRule.getRule().getVersionCsiStd()
                    + controlRule.getRule().getVersionCsiSpecific()
                    + controlRule.getRule().getMachineSoftwareVersion()
                    + controlRule.getRule().getMachineMasterSoftwareVersion();
            broadcastState.put(scope, controlRule.getRule());
            if (ruleKey != null) {
                broadcastState.remove(ruleKey);
            }
        } else if (controlRule.getControl().equals(Control.INACTIVE)) {
            if (ruleKey != null) {
                broadcastState.remove(ruleKey);
            }
        }
    }
}
