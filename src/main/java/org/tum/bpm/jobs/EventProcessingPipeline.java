package org.tum.bpm.jobs;

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
import org.tum.bpm.schemas.AbstractedEvent;
import org.tum.bpm.schemas.CorrelatedEvent;
import org.tum.bpm.schemas.EquipmentListEvent;
import org.tum.bpm.schemas.Resource;
import org.tum.bpm.schemas.ScopedMeasurement;
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
import org.tum.bpm.schemas.stats.Alarm;
import org.tum.bpm.sinks.AlarmSink;
import org.tum.bpm.sinks.KafkaBpmSink;
import org.tum.bpm.sinks.MongoBpmSink;
import org.tum.bpm.sinks.dynamicMongoSink.MetaDocument;
import org.tum.bpm.sources.MeasurementKafkaSource;
import org.tum.bpm.sources.RulesSource;

public class EventProcessingPipeline {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // env.enableCheckpointing(30000);

        DataStream<IoTMessageSchema> iotMessageStream = env
                .fromSource(MeasurementKafkaSource.createMeasurementSource(),
                        MeasurementKafkaSource.createWatermarkStrategy(),
                        "Measurement Source")
                .name("IoT-Message-Stream");

        DataStream<MongoChangeStreamMessage> ruleStream = env
                .fromSource(RulesSource.createRulesIncrementalSource(),
                        RulesSource.createWatermarkStrategy(), "Rule Bootstrap Source")
                .setParallelism(1)
                .process(new MongoChangeStreamDeserialization())
                .name("Rule-Stream");

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
        DataStream<ScopedMeasurement> scopedIoTMessageStream = iotMessageStream
                .connect(eventScopingRuleBroadcast)
                .process(new DynamicScopeFunction())
                .name("Scoped Stream");


        // 1. Key by scope, edge device id and variable name -> each measurement is now
        // uniquely
        // identifiable and interpretable
        // 2. The dynamic event abstraction function creates the event, based on the
        // abstraction rule stream.
        DataStream<AbstractedEvent> events = scopedIoTMessageStream
                .keyBy(message -> message.getScope()
                        + message.getIotMessage().getPayload().getEdgeDeviceId()
                        + message.getIotMessage().getPayload().getVarName())
                .connect(eventAbstractionRuleBroadcast)
                .process(new DynamicEventAbstractionFunction())
                .name("Event Stream");

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
                .process(new EventBatchEnrichmentFunction())
                .name("Enriched-Event Stream");
        
        SingleOutputStreamOperator<CorrelatedEvent> correlatedEvents = enrichedEvents
                .keyBy(enrichedEvent -> enrichedEvent.getEvent()
                        .getIotMessage().getPayload().getEdgeDeviceId())
                .process(new EventResourceCorrelationFunction())
                .name("Correlated-Event Stream");

        DataStream<Resource> resourceStream = correlatedEvents
                .getSideOutput(EventResourceCorrelationFunction.RESOURCE_OUTPUT_TAG);

        DataStream<Alarm> alarmStream = correlatedEvents
                .getSideOutput(EventResourceCorrelationFunction.ALARM_OUTPUT_TAG);

        DataStream<MetaDocument<OcelEvent>> ocelEvents = correlatedEvents.map(new OcelEventSerialization()).name("Ocel-Event Stream");
        DataStream<OcelObject> ocelObjects = resourceStream.connect(resourceNameRuleBroadcast)
                .process(new OcelObjectSerialization()).name("Ocel-Object Stream");

        alarmStream.sinkTo(AlarmSink.createAlarmSink());
        ocelEvents.sinkTo(KafkaBpmSink.createOcelEventSink());
        ocelEvents.sinkTo(MongoBpmSink.createOcelEventSink());
        ocelObjects.sinkTo(KafkaBpmSink.createOcelObjectSink());
        env.execute("Event processing pipeline");
    }
}