package org.tum.bpm.functions.enrichment;

import java.util.Map;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.KeyedCoProcessFunction;
import org.apache.flink.util.Collector;
import org.tum.bpm.schemas.EquipmentListEvent;
import org.tum.bpm.schemas.measurements.CSIMeasurement;
import org.tum.bpm.schemas.measurements.IoTMessageSchema;
import org.tum.bpm.schemas.AttributeEvent;

public class DynamicEventBatchEnrichmentFunction
        extends
        KeyedCoProcessFunction<String, IoTMessageSchema, EquipmentListEvent<IoTMessageSchema>, AttributeEvent<String>> {

    private transient MapState<String, TreeSet<IoTMessageSchema>> measurementBuffer;
    private transient ListState<EquipmentListEvent<IoTMessageSchema>> eventBuffer;

    private MapStateDescriptor<String, TreeSet<IoTMessageSchema>> measurementBufferDescriptor = new MapStateDescriptor<>(
            "measurementBuffer",
            BasicTypeInfo.STRING_TYPE_INFO,
            TypeInformation.of(new TypeHint<TreeSet<IoTMessageSchema>>() {
            }));

    private ListStateDescriptor<EquipmentListEvent<IoTMessageSchema>> eventBufferDescriptor = new ListStateDescriptor<>(
            "eventBuffer",
            TypeInformation.of(new TypeHint<EquipmentListEvent<IoTMessageSchema>>() {
            }));

    @Override
    public void open(Configuration parameters) {
        this.measurementBuffer = getRuntimeContext().getMapState(measurementBufferDescriptor);
        this.eventBuffer = getRuntimeContext().getListState(eventBufferDescriptor);
    }

    @Override
    public void processElement1(IoTMessageSchema value,
            KeyedCoProcessFunction<String, IoTMessageSchema, EquipmentListEvent<IoTMessageSchema>, AttributeEvent<String>>.Context ctx,
            Collector<AttributeEvent<String>> out) throws Exception {

        TreeSet<IoTMessageSchema> fieldBuffer = this.measurementBuffer.get(value.getPayload().getVarName());
        if (fieldBuffer == null)
            fieldBuffer = new TreeSet<IoTMessageSchema>();
        fieldBuffer.add(value);
        this.measurementBuffer.put(value.getPayload().getVarName(), fieldBuffer);
    }

    @Override
    public void processElement2(EquipmentListEvent<IoTMessageSchema> value,
            KeyedCoProcessFunction<String, IoTMessageSchema, EquipmentListEvent<IoTMessageSchema>, AttributeEvent<String>>.Context ctx,
            Collector<AttributeEvent<String>> out) throws Exception {
        this.eventBuffer.add(value);
        ctx.timerService().registerEventTimeTimer(ctx.timestamp());
    }

    @Override
    public void onTimer(long timestamp, OnTimerContext ctx,
            Collector<AttributeEvent<String>> out)
            throws Exception {

        // For each buffered event enrich the event with attributes according to the
        // enrichment rules
        for (EquipmentListEvent<IoTMessageSchema> event : this.eventBuffer.get()) {
            Map<String, String> enrichment = new HashMap<>();
            for (String field : event.getStatusFields()) {
                TreeSet<IoTMessageSchema> fieldBuffer = this.measurementBuffer.get(field);
                if (fieldBuffer != null) {
                    IoTMessageSchema mostRecentMeasurement = fieldBuffer.floor(event.getBaseEvent().getMessage());
                    enrichment.put(field, mostRecentMeasurement.getPayload().getTimestampUtc().toString()); // Can be null
                }
            }
            out.collect(new AttributeEvent<String>(UUID.randomUUID().toString(),
                    event.getBaseEvent().getRule().getEventName(), event.getBaseEvent().getMessage().getPayload().getTimestampUtc(), enrichment));
        }
        this.eventBuffer.clear();

        // TODO: don't clean the latest measurement

        ZonedDateTime waterMarkTime = ZonedDateTime
                .ofInstant(Instant.ofEpochMilli(ctx.timerService().currentWatermark()), ZoneId.of("UTC"));
        IoTMessageSchema dummyMeasurement = new IoTMessageSchema();
        dummyMeasurement.setPayload(new CSIMeasurement());
        dummyMeasurement.getPayload().setTimestampUtc(waterMarkTime);
        for (Map.Entry<String, TreeSet<IoTMessageSchema>> fieldBuffer : this.measurementBuffer.entries()) {
            fieldBuffer.getValue().headSet(dummyMeasurement, false).clear();
            this.measurementBuffer.put(fieldBuffer.getKey(), fieldBuffer.getValue());
        }
    }
}
