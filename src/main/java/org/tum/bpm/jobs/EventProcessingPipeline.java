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
import org.tum.bpm.functions.scoping.DynamicScopeFunction;
import org.tum.bpm.functions.serialization.BootstrapRuleAlignmentFunction;
import org.tum.bpm.functions.serialization.ChangeRuleDeserializationFunction;
import org.tum.bpm.schemas.AttributeEvent;
import org.tum.bpm.schemas.BaseEvent;
import org.tum.bpm.schemas.EquipmentListEvent;
import org.tum.bpm.schemas.Scoped;
import org.tum.bpm.schemas.measurements.IoTMessageSchema;
import org.tum.bpm.schemas.rules.EventAbstractionRule;
import org.tum.bpm.schemas.rules.EventEnrichmentRule;
import org.tum.bpm.schemas.rules.EventScopingRule;
import org.tum.bpm.schemas.rules.Rule;
import org.tum.bpm.sources.MeasurementKafkaSource;
import org.tum.bpm.sources.RulesSource;

public class EventProcessingPipeline {

    public static void main(String[] args) throws Exception {
        try {
            final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
            // env.enableCheckpointing(3000);

            DataStream<IoTMessageSchema> iotMessageStream = env
                    .fromSource(MeasurementKafkaSource.createMeasurementSource(),
                            MeasurementKafkaSource.createWatermarkStrategy(),
                            "Measurement Source");

            DataStream<String> ruleStream = env
                    .fromSource(RulesSource.createRulesIncrementalSource(),
                            RulesSource.createWatermarkStrategy(), "Rule Bootstrap Source")
                    .setParallelism(1).process(new BootstrapRuleAlignmentFunction());

            SingleOutputStreamOperator<Rule> splitRuleStream = ruleStream
                    .process(new ChangeRuleDeserializationFunction());

            BroadcastStream<EventScopingRule> eventScopingRuleBroadcast = splitRuleStream
                    .getSideOutput(ChangeRuleDeserializationFunction.EVENT_SCOPING_RULE_OUTPUT_TAG)
                    .broadcast(DynamicScopeFunction.SCOPE_RULES_BROADCAST_STATE_DESCRIPTOR);
            BroadcastStream<EventAbstractionRule> eventAbstractionRuleBroadcast = splitRuleStream
                    .getSideOutput(ChangeRuleDeserializationFunction.EVENT_ABSTRACTION_RULE_OUTPUT_TAG)
                    .broadcast(DynamicEventAbstractionFunction.ABSTRACTION_RULES_BROADCAST_STATE_DESCRIPTOR);
            BroadcastStream<EventEnrichmentRule> eventEnrichmentRuleBroadcast = splitRuleStream
                    .getSideOutput(ChangeRuleDeserializationFunction.EVENT_ENRICHMENT_RULE_OUTPUT_TAG)
                    .broadcast(DynamicEventEnrichmentPreparationFunction.ENRICHMENT_RULES_BROADCAST_STATE_DESCRIPTOR);

            // Connect Scoping rule Stream and assign a Scope to each IoTMeasurement
            DataStream<Scoped<IoTMessageSchema, String>> scopedIoTMessageStream = iotMessageStream
                    .connect(eventScopingRuleBroadcast)
                    .process(new DynamicScopeFunction());

            // 1. Key by scope and edge device id -> each measurement is now uniquely
            // identifiable and interpretable
            // 2. The dynamic event abstraction function creates the event, based on the
            // abstraction rule stream.
            DataStream<BaseEvent<IoTMessageSchema>> events = scopedIoTMessageStream
                    .keyBy(message -> message.getScope()
                            + message.getWrapped().getPayload().getEdgeDeviceId())
                    .connect(eventAbstractionRuleBroadcast)
                    .process(new DynamicEventAbstractionFunction());

            // We cannot combine a coprocess function and a broadcast function at once, so
            // we have to prepare it like this. The
            // DynamicEventEnrichmentPreparationFunction adds
            // an array of statusfields that are necessary for enrichment
            DataStream<EquipmentListEvent<IoTMessageSchema>> equipmentListEvents = events
                    .keyBy(event -> event.getRule().getEquipmentId())
                    .connect(eventEnrichmentRuleBroadcast)
                    .process(new DynamicEventEnrichmentPreparationFunction());

            
            DataStream<AttributeEvent<String>> enrichedEvents = iotMessageStream
                    .connect(equipmentListEvents)
                    .keyBy(measurement -> measurement.getPayload().getEdgeDeviceId()
                            + measurement.getPayload().getMachineId(),
                            event -> event.getBaseEvent().getMessage().getPayload()
                                    .getEdgeDeviceId()
                                    + event.getBaseEvent().getMessage().getPayload()
                                            .getMachineId(),
                            BasicTypeInfo.STRING_TYPE_INFO)
                    .process(new DynamicEventBatchEnrichmentFunction());

            // scopedIoTMessageStream.print();
            enrichedEvents.print();
            // changeRuleStream.print();
            // events.print();
            env.execute("Testing flink consumer");
        } catch (FileNotFoundException e) {
            System.out.println("FileNoteFoundException: " + e);
        }
    }

}
