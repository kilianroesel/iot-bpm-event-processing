package org.tum.bpm.jobs;

import java.io.FileNotFoundException;

import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import org.tum.bpm.functions.abstraction.DynamicEventAbstractionFunction;
import org.tum.bpm.functions.enrichment.DynamicEventBatchEnrichmentFunction;
import org.tum.bpm.functions.enrichment.DynamicEventEnrichmentPreparationFunction;
import org.tum.bpm.functions.ruleRefinement.RuleRefinementFunction;
import org.tum.bpm.functions.scoping.DynamicScopeFunction;
import org.tum.bpm.functions.serialization.DebeziumDeserializationFunction;
import org.tum.bpm.schemas.AttributeEvent;
import org.tum.bpm.schemas.BaseEvent;
import org.tum.bpm.schemas.EquipmentListEvent;
import org.tum.bpm.schemas.Scoped;
import org.tum.bpm.schemas.debezium.DebeziumMessage;
import org.tum.bpm.schemas.measurements.IoTMessageSchema;
import org.tum.bpm.schemas.rules.EventAbstractionRule;
import org.tum.bpm.schemas.rules.EventEnrichmentRule;
import org.tum.bpm.schemas.rules.EventScopingRule;
import org.tum.bpm.schemas.rules.RuleControl;
import org.tum.bpm.sources.MeasurementKafkaSource;
import org.tum.bpm.sources.RulesSource;

public class EventProcessingPipeline {

    public static void main(String[] args) throws Exception {
        try {
            final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
            env.enableCheckpointing(3000);

            DataStream<IoTMessageSchema> iotMessageStream = env
                    .fromSource(MeasurementKafkaSource.createMeasurementSource(),
                            MeasurementKafkaSource.createWatermarkStrategy(),
                            "Measurement Source");

            // Stream listening to changes in the Rule tables
            DataStream<String> changeRuleStream = env
                .fromSource(RulesSource.creatRulesSource(),
                RulesSource.createWatermarkStrategy(), "Rule Source")
                .setParallelism(1);

            // Deserializing Debezium JSON String and splitting into different ruleTypes
            SingleOutputStreamOperator<DebeziumMessage<?>> splitRuleStream = changeRuleStream
                    .process(new DebeziumDeserializationFunction());

            // Transforming Debezium Message EventRule messages into
            // RuleControl<EventScopingRule>. The rule control wrapper depicts what to do
            // with the rule in the broadcast process function
            DataStream<DebeziumMessage<EventScopingRule>> eventScopingRuleUpdates = splitRuleStream
                    .getSideOutput(DebeziumDeserializationFunction.RAW_EVENT_SCOPING_RULE_OUTPUT_TAG);
            DataStream<RuleControl<EventScopingRule>> eventScopingRule = eventScopingRuleUpdates
                    .flatMap(new RuleRefinementFunction<EventScopingRule>());
            BroadcastStream<RuleControl<EventScopingRule>> eventScopingRuleBroadcastStream = eventScopingRule
                    .broadcast(DynamicScopeFunction.SCOPE_RULES_BROADCAST_STATE_DESCRIPTOR);

            DataStream<DebeziumMessage<EventAbstractionRule>> eventAbstractionRuleUpdates = splitRuleStream
                    .getSideOutput(DebeziumDeserializationFunction.RAW_EVENT_ABSTRACTION_RULE_OUTPUT_TAG);
            DataStream<RuleControl<EventAbstractionRule>> eventAbstractionRule = eventAbstractionRuleUpdates
                    .flatMap(new RuleRefinementFunction<EventAbstractionRule>());
            BroadcastStream<RuleControl<EventAbstractionRule>> eventAbstractionRuleBroadcastStream = eventAbstractionRule
                    .broadcast(DynamicEventAbstractionFunction.ABSTRACTION_RULES_BROADCAST_STATE_DESCRIPTOR);

            DataStream<DebeziumMessage<EventEnrichmentRule>> eventEnrichmentRuleUpdates = splitRuleStream
                    .getSideOutput(DebeziumDeserializationFunction.RAW_EVENT_ENRICHMENT_RULE_OUTPUT_TAG);
            DataStream<RuleControl<EventEnrichmentRule>> eventEnrichmentRule = eventEnrichmentRuleUpdates
                    .flatMap(new RuleRefinementFunction<EventEnrichmentRule>());
            BroadcastStream<RuleControl<EventEnrichmentRule>> eventEnrichmentRuleBroadcastStream = eventEnrichmentRule
                    .broadcast(DynamicEventEnrichmentPreparationFunction.ENRICHMENT_RULES_BROADCAST_STATE_DESCRIPTOR);

            // Connect Scoping rule Stream and assign a Scope to each IoTMeasurement
            DataStream<Scoped<IoTMessageSchema, String>> scopedIoTMessageStream = iotMessageStream
                    .connect(eventScopingRuleBroadcastStream)
                    .process(new DynamicScopeFunction());

            DataStream<BaseEvent<IoTMessageSchema>> events = scopedIoTMessageStream
                    .keyBy(message -> message.getScope() + message.getWrapped().getPayload().getEdgeDeviceId())
                    .connect(eventAbstractionRuleBroadcastStream)
                    .process(new DynamicEventAbstractionFunction());

            // We cannot combine a coprocess function and a broadcast function at once, so
            // we have to prepare it like this
            DataStream<EquipmentListEvent<IoTMessageSchema>> equipmentListEvents = events
                    .keyBy(event -> event.getRule().getEquipmentId())
                    .connect(eventEnrichmentRuleBroadcastStream)
                    .process(new DynamicEventEnrichmentPreparationFunction());

            DataStream<AttributeEvent<IoTMessageSchema>> enrichedEvents = iotMessageStream
                    .connect(equipmentListEvents)
                    .keyBy(measurement -> measurement.getPayload().getEdgeDeviceId()
                            + measurement.getPayload().getMachineId(),
                            event -> event.getBaseEvent().getMessage().getPayload().getEdgeDeviceId()
                                    + event.getBaseEvent().getMessage().getPayload().getMachineId(),
                            BasicTypeInfo.STRING_TYPE_INFO)
                    .process(new DynamicEventBatchEnrichmentFunction());

            enrichedEvents.print();
            env.execute("Testing flink consumer");
        } catch (FileNotFoundException e) {
            System.out.println("FileNoteFoundException: " + e);
        }
    }

}
