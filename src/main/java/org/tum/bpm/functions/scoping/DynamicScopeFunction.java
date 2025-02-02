package org.tum.bpm.functions.scoping;

import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.tum.bpm.schemas.Scoped;
import org.tum.bpm.schemas.measurements.IoTMessageSchema;
import org.tum.bpm.schemas.rules.EventScopingRule;
import org.tum.bpm.schemas.rules.RuleControl;

import java.util.Map;

public class DynamicScopeFunction
        extends BroadcastProcessFunction<IoTMessageSchema, RuleControl<EventScopingRule>, Scoped<IoTMessageSchema, String>> {

    // Output Tag for Measurements that do not have a registered scope
    public static final OutputTag<IoTMessageSchema> NO_SCOPE_MEASUREMENT_OUTPUT_TAG = new OutputTag<IoTMessageSchema>(
            "noScopeMeasurementOutputTag") {
    };

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
                + measurement.getPayload().getVersionCsiSpecific();

        EventScopingRule rule = rulesState.get(scope);
        if (rule == null) {
            ctx.output(NO_SCOPE_MEASUREMENT_OUTPUT_TAG, measurement);
        } else {
            out.collect(new Scoped<IoTMessageSchema, String>(measurement, rule.getRuleId()));
        }
    }

    @Override
    public void processBroadcastElement(RuleControl<EventScopingRule> ruleControl,
            BroadcastProcessFunction<IoTMessageSchema, RuleControl<EventScopingRule>, Scoped<IoTMessageSchema, String>>.Context ctx,
            Collector<Scoped<IoTMessageSchema, String>> out) throws Exception {

                EventScopingRule rule = ruleControl.getRule();
        BroadcastState<String, EventScopingRule> broadcastState = ctx
                .getBroadcastState(SCOPE_RULES_BROADCAST_STATE_DESCRIPTOR);

        // Retrieve current rule key with given scopeId
        String ruleKey = null;
        for (Map.Entry<String, EventScopingRule> entry : broadcastState.entries()) {
            if (entry.getValue().getRuleId().equals(rule.getRuleId())) {
                ruleKey = entry.getKey();
            }
        }
        // Update broadcast rule state depending on rule control
        if (ruleControl.getControl() == RuleControl.Control.ACTIVE) {
            String scope = rule.getMachineName()
                    + rule.getVersionCsiStd()
                    + rule.getVersionCsiSpecific();
                    
            broadcastState.put(scope, rule);
            if (ruleKey != null) {
                broadcastState.remove(ruleKey);
            }
        } else if (ruleControl.getControl().equals(RuleControl.Control.INACTIVE)) {
            if (ruleKey != null) {
                broadcastState.remove(ruleKey);
            }
        }
    }
}
