package org.tum.bpm.functions.enrichment;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Gauge;
import org.apache.flink.streaming.api.functions.co.KeyedCoProcessFunction;
import org.apache.flink.util.Collector;
import org.tum.bpm.schemas.EquipmentListEvent;
import org.tum.bpm.schemas.measurements.IoTMessageSchema;
import org.tum.bpm.schemas.ocel.OcelAttribute;
import org.tum.bpm.schemas.rules.EventEnrichmentRule;
import org.tum.bpm.schemas.EnrichedEvent;

public class EventBatchEnrichmentFunction
        extends
        KeyedCoProcessFunction<String, IoTMessageSchema, EquipmentListEvent, EnrichedEvent> {

    private transient MapState<String, TreeSet<IoTMessageSchema>> measurementBuffer;
    private transient ListState<EquipmentListEvent> eventBuffer;

    private MapStateDescriptor<String, TreeSet<IoTMessageSchema>> measurementBufferDescriptor = new MapStateDescriptor<>(
            "measurementBuffer",
            BasicTypeInfo.STRING_TYPE_INFO,
            TypeInformation.of(new TypeHint<TreeSet<IoTMessageSchema>>() {
            }));

    private ListStateDescriptor<EquipmentListEvent> eventBufferDescriptor = new ListStateDescriptor<>(
            "eventBuffer",
            TypeInformation.of(new TypeHint<EquipmentListEvent>() {
            }));

    @Override
    public void open(Configuration parameters) {
        this.measurementBuffer = getRuntimeContext().getMapState(measurementBufferDescriptor);
        this.eventBuffer = getRuntimeContext().getListState(eventBufferDescriptor);
        getRuntimeContext().getMetricGroup().gauge("eventEnrichmentBufferGauge", (Gauge<Integer>) () -> {
            try {
                int totalSize = 0;
                for (TreeSet<IoTMessageSchema> buffer : this.measurementBuffer.values()) {
                    totalSize += buffer.size();
                }
                return totalSize;
            } catch (Exception e) {
                return 0;
            }
        });
    }

    @Override
    public void processElement1(IoTMessageSchema iotMessage,
            KeyedCoProcessFunction<String, IoTMessageSchema, EquipmentListEvent, EnrichedEvent>.Context ctx,
            Collector<EnrichedEvent> out) throws Exception {

        TreeSet<IoTMessageSchema> fieldBuffer = this.measurementBuffer.get(iotMessage.getPayload().getVarName());
        if (fieldBuffer == null)
            fieldBuffer = new TreeSet<IoTMessageSchema>();
        fieldBuffer.add(iotMessage);
        if (fieldBuffer.size() > 10) {
            fieldBuffer.pollFirst(); // Remove to keep at most the last 10 measurements
        }
        this.measurementBuffer.put(iotMessage.getPayload().getVarName(), fieldBuffer);
    }

    @Override
    public void processElement2(EquipmentListEvent value,
            KeyedCoProcessFunction<String, IoTMessageSchema, EquipmentListEvent, EnrichedEvent>.Context ctx,
            Collector<EnrichedEvent> out) throws Exception {
        this.eventBuffer.add(value);
        ctx.timerService().registerEventTimeTimer(ctx.timerService().currentWatermark()+100);
    }

    @Override
    public void onTimer(long timestamp, OnTimerContext ctx,
            Collector<EnrichedEvent> out)
            throws Exception {

        List<EquipmentListEvent> remainingEvents = new ArrayList<>();
        // For each buffered event enrich the event with attributes according to the
        // enrichment rules
        for (EquipmentListEvent event : this.eventBuffer.get()) {
            List<OcelAttribute> enrichment = new ArrayList<OcelAttribute>();
            if (event.getEnrichmentRules() != null) {
                for (EventEnrichmentRule eventEnrichmentRule : event.getEnrichmentRules()) {
                    TreeSet<IoTMessageSchema> fieldBuffer = this.measurementBuffer.get(eventEnrichmentRule.getField());
                    if (fieldBuffer != null) {
                        IoTMessageSchema mostRecentMeasurement = fieldBuffer.floor(event.getBaseEvent().getIotMessage());
                        if (mostRecentMeasurement != null && mostRecentMeasurement.getPayload() != null)
                            enrichment.add(new OcelAttribute(eventEnrichmentRule.getStatusName(), mostRecentMeasurement.getPayload().getVarValue()));
                    }
                }
            }
            out.collect(new EnrichedEvent(event.getBaseEvent(), enrichment));
        }
        this.eventBuffer.clear();
        this.eventBuffer.update(remainingEvents);
    }
}
