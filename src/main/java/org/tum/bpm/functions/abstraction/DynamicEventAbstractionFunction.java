package org.tum.bpm.functions.abstraction;

import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.util.List;
import java.util.ArrayList;

import org.tum.bpm.schemas.rules.EventAbstractionRule;
import org.tum.bpm.schemas.rules.RuleControl;
import org.tum.bpm.schemas.BaseEvent;
import org.tum.bpm.schemas.Scoped;
import org.tum.bpm.schemas.measurements.IoTMessageSchema;

public class DynamicEventAbstractionFunction extends
        KeyedBroadcastProcessFunction<String, Scoped<IoTMessageSchema, String>, RuleControl<EventAbstractionRule>, BaseEvent> {

    private transient ValueState<IoTMessageSchema> lastMeasurementState;

    // Keyed state storing the last value of the encountered IoTMessageSchema
    private final ValueStateDescriptor<IoTMessageSchema> LAST_MEASUREMENT_STATE_DESCRIPTOR = new ValueStateDescriptor<>(
            "lastMeasurementStateDescriptor",
            TypeInformation.of(new TypeHint<IoTMessageSchema>() {
            }));

    // Broadcast state
    public static final MapStateDescriptor<String, List<EventAbstractionRule>> ABSTRACTION_RULES_BROADCAST_STATE_DESCRIPTOR = new MapStateDescriptor<String, List<EventAbstractionRule>>(
            "abstractionRulesBroadcastState",
            BasicTypeInfo.STRING_TYPE_INFO,
            TypeInformation.of(new TypeHint<List<EventAbstractionRule>>() {
            }));

    @Override
    public void open(Configuration parameters) {
        this.lastMeasurementState = getRuntimeContext().getState(LAST_MEASUREMENT_STATE_DESCRIPTOR);
    }

    @Override
    public void processElement(Scoped<IoTMessageSchema, String> measurement,
            KeyedBroadcastProcessFunction<String, Scoped<IoTMessageSchema, String>, RuleControl<EventAbstractionRule>, BaseEvent>.ReadOnlyContext ctx,
            Collector<BaseEvent> out) throws Exception {

        ReadOnlyBroadcastState<String, List<EventAbstractionRule>> abstractionRuleState = ctx
                .getBroadcastState(ABSTRACTION_RULES_BROADCAST_STATE_DESCRIPTOR);
        IoTMessageSchema lastValue = this.lastMeasurementState.value();
        if (lastValue != null) {
            List<EventAbstractionRule> rules = abstractionRuleState
                    .get(measurement.getScope() + measurement.getWrapped().getPayload().getVarName());
            if (rules != null) {
                for (EventAbstractionRule rule : rules) {
                    if (this.evaluateRule(rule, measurement.getWrapped(), lastValue))
                        out.collect(new BaseEvent(rule, measurement.getWrapped()));
                }
            }
        }
        this.lastMeasurementState.update(measurement.getWrapped());
    }

    @Override
    public void processBroadcastElement(RuleControl<EventAbstractionRule> ruleControl,
            KeyedBroadcastProcessFunction<String, Scoped<IoTMessageSchema, String>, RuleControl<EventAbstractionRule>, BaseEvent>.Context ctx,
            Collector<BaseEvent> out) throws Exception {

        EventAbstractionRule rule = ruleControl.getRule();
        BroadcastState<String, List<EventAbstractionRule>> broadcastState = ctx
                .getBroadcastState(ABSTRACTION_RULES_BROADCAST_STATE_DESCRIPTOR);
        List<EventAbstractionRule> rules = broadcastState
                .get(rule.getScopeId() + rule.getField());

        if (rules == null) {
            rules = new ArrayList<>();
        }
        switch (ruleControl.getControl()) {
            case ACTIVE:
                rules.removeIf(currentRule -> currentRule.getRuleId().equals(rule.getRuleId()));
                rules.add(rule);
                break;
            case INACTIVE:
                rules.removeIf(currentRule -> currentRule.getRuleId().equals(rule.getRuleId()));
                break;
            default:
                break;
        }
        broadcastState.put(rule.getScopeId() + rule.getField(), rules);
    }

    private boolean evaluateRule(EventAbstractionRule rule, IoTMessageSchema measurement,
            IoTMessageSchema lastMeasurement) {
        Double currentValue = Double.parseDouble(measurement.getPayload().getVarValue());
        Double lastValue = Double.parseDouble(lastMeasurement.getPayload().getVarValue());
        switch (rule.getTriggerType()) {
            case "CHANGES_TO":
                return rule.getValue() == currentValue
                        && currentValue != lastValue;
            case "CHANGES_FROM":
                return rule.getValue() != currentValue
                        && rule.getValue() == lastValue;
            case "INCREASES_BY":
                return currentValue - lastValue >= rule.getValue();
            case "DECREASES_BY":
                return currentValue - lastValue <= rule.getValue();
            case "ABSOLUTE_CHANGE_IS_EQUAL":
                return Math.abs(currentValue - lastValue) == rule.getValue();
            case "ABSOLUTE_CHANGE_IS_GREATER_EQUAL":
                return Math.abs(currentValue - lastValue) >= rule.getValue();
            case "CHANGE_IS_GREATER_EQUAL":
                return (currentValue - lastValue) >= rule.getValue();
            case "ENTERS_RANGE_FROM_TO":
                return (rule.getFrom() <= currentValue && rule.getTo() >= currentValue) && (rule.getFrom() > lastValue || rule.getTo() < lastValue);
            case "LEAVES_RANGE_FROM_TO":
                return (rule.getFrom() <= lastValue && rule.getTo() >= lastValue) && (rule.getFrom() > currentValue || rule.getTo() < currentValue);
            default:
                return false;
        }
    }
}
