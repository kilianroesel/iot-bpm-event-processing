package org.tum.bpm.jobs;

import java.io.FileNotFoundException;

import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.tum.bpm.functions.abstraction.DynamicEventAbstractionFunction;
import org.tum.bpm.functions.correlation.EventResourceCorrelationFunction;
import org.tum.bpm.functions.deserialization.MongoChangeStreamDeserialization;
import org.tum.bpm.functions.deserialization.RulesDeserialization;
import org.tum.bpm.functions.enrichment.EventBatchEnrichmentFunction;
import org.tum.bpm.functions.enrichment.DynamicEventEnrichmentPreparationFunction;
import org.tum.bpm.functions.scoping.DynamicScopeFunction;
import org.tum.bpm.functions.serialization.OcelEventSerialization;
import org.tum.bpm.functions.serialization.OcelObjectSerialization;
import org.tum.bpm.schemas.EnrichedEvent;
import org.tum.bpm.schemas.BaseEvent;
import org.tum.bpm.schemas.CorrelatedEvent;
import org.tum.bpm.schemas.EquipmentListEvent;
import org.tum.bpm.schemas.Resource;
import org.tum.bpm.schemas.Scoped;
import org.tum.bpm.schemas.debezium.MongoChangeStreamMessage;
import org.tum.bpm.schemas.measurements.IoTMessageSchema;
import org.tum.bpm.schemas.ocel.OcelEvent;
import org.tum.bpm.schemas.ocel.OcelObject;
import org.tum.bpm.schemas.rules.EventAbstractionRule;
import org.tum.bpm.schemas.rules.EventEnrichmentRule;
import org.tum.bpm.schemas.rules.EventScopingRule;
import org.tum.bpm.schemas.rules.ResourceNameRule;
import org.tum.bpm.schemas.rules.Rule;
import org.tum.bpm.schemas.rules.RuleControl;
import org.tum.bpm.sinks.KafkaBpmSink;
import org.tum.bpm.sinks.MongoBpmSink;
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

            DataStream<MongoChangeStreamMessage> ruleStream = env
                    .fromSource(RulesSource.createRulesIncrementalSource(),
                            RulesSource.createWatermarkStrategy(), "Rule Bootstrap Source")
                    .setParallelism(1)
                    .process(new MongoChangeStreamDeserialization());

            SingleOutputStreamOperator<RuleControl<Rule>> splitRuleStream = ruleStream
                    .process(new RulesDeserialization());

            BroadcastStream<RuleControl<EventScopingRule>> eventScopingRuleBroadcast = splitRuleStream
                    .getSideOutput(RulesDeserialization.EVENT_SCOPING_RULE_OUTPUT_TAG)
                    .broadcast(DynamicScopeFunction.SCOPE_RULES_BROADCAST_STATE_DESCRIPTOR);
            BroadcastStream<RuleControl<EventAbstractionRule>> eventAbstractionRuleBroadcast = splitRuleStream
                    .getSideOutput(RulesDeserialization.EVENT_ABSTRACTION_RULE_OUTPUT_TAG)
                    .broadcast(DynamicEventAbstractionFunction.ABSTRACTION_RULES_BROADCAST_STATE_DESCRIPTOR);
            BroadcastStream<RuleControl<EventEnrichmentRule>> eventEnrichmentRuleBroadcast = splitRuleStream
                    .getSideOutput(RulesDeserialization.EVENT_ENRICHMENT_RULE_OUTPUT_TAG)
                    .broadcast(DynamicEventEnrichmentPreparationFunction.ENRICHMENT_RULES_BROADCAST_STATE_DESCRIPTOR);
            BroadcastStream<RuleControl<ResourceNameRule>> resourceNameRuleBroadcast = splitRuleStream
                    .getSideOutput(RulesDeserialization.RESOURCE_NAME_RULE_OUTPUT_TAG)
                    .broadcast(OcelObjectSerialization.RESOURCE_NAME_RULE_BROADCAST_STATE_DESCRIPTOR);

            // Connect Scoping rule Stream and assign a Scope to each IoTMeasurement
            DataStream<Scoped<IoTMessageSchema, String>> scopedIoTMessageStream = iotMessageStream
                    .connect(eventScopingRuleBroadcast)
                    .process(new DynamicScopeFunction());

            // 1. Key by scope and edge device id -> each measurement is now uniquely
            // identifiable and interpretable
            // 2. The dynamic event abstraction function creates the event, based on the
            // abstraction rule stream.
            DataStream<BaseEvent> events = scopedIoTMessageStream
                    .keyBy(message -> message.getScope() + message.getWrapped().getPayload().getEdgeDeviceId()
                            + message.getWrapped().getPayload().getVarName())
                    .connect(eventAbstractionRuleBroadcast)
                    .process(new DynamicEventAbstractionFunction());

            // We cannot combine a coprocess function and a broadcast function at once, so
            // we have to prepare it like this. The
            // DynamicEventEnrichmentPreparationFunction adds
            // an array of statusfields that are necessary for enrichment
            DataStream<EquipmentListEvent> equipmentListEvents = events
                    .connect(eventEnrichmentRuleBroadcast)
                    .process(new DynamicEventEnrichmentPreparationFunction());

            DataStream<EnrichedEvent> enrichedEvents = iotMessageStream
                    .connect(equipmentListEvents)
                    .keyBy(measurement -> measurement.getPayload().getEdgeDeviceId()
                            + measurement.getPayload().getMachineId(),
                            event -> event.getBaseEvent().getIotMessage().getPayload()
                                    .getEdgeDeviceId()
                                    + event.getBaseEvent().getIotMessage().getPayload()
                                            .getMachineId(),
                            BasicTypeInfo.STRING_TYPE_INFO)
                    .process(new EventBatchEnrichmentFunction());

            SingleOutputStreamOperator<CorrelatedEvent> correlatedEvents = enrichedEvents
                    .keyBy(enrichedEvent -> enrichedEvent.getEvent()
                            .getIotMessage().getPayload().getEdgeDeviceId())
                    .process(new EventResourceCorrelationFunction());

            DataStream<Resource> resourceStream = correlatedEvents
                    .getSideOutput(EventResourceCorrelationFunction.RESOURCE_OUTPUT_TAG);

            DataStream<OcelEvent> ocelEvents = correlatedEvents.map(new OcelEventSerialization());
            DataStream<OcelObject> ocelObjects = resourceStream.connect(resourceNameRuleBroadcast)
                    .process(new OcelObjectSerialization());

            ocelEvents.sinkTo(MongoBpmSink.createOcelEventSink());
            ocelEvents.sinkTo(KafkaBpmSink.createOcelEventSink());
            ocelObjects.sinkTo(KafkaBpmSink.createOcelObjectSink());
            env.execute("Testing flink consumer");
        } catch (FileNotFoundException e) {
            System.out.println("FileNoteFoundException: " + e);
        }
    }
}