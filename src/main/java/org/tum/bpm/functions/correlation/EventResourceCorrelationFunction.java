package org.tum.bpm.functions.correlation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Queue;

import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.tum.bpm.schemas.CorrelatedEvent;
import org.tum.bpm.schemas.EnrichedEvent;
import org.tum.bpm.schemas.Resource;
import org.tum.bpm.schemas.ocel.OcelRelationship;
import org.tum.bpm.schemas.rules.EventAbstractionRule.EventResourceRelation;

public class EventResourceCorrelationFunction extends KeyedProcessFunction<String, EnrichedEvent, CorrelatedEvent> {

    private transient MapState<String, Queue<Resource>> resourceQueues;
    private MapStateDescriptor<String, Queue<Resource>> resourceQueueDescriptor = new MapStateDescriptor<>(
            "resourceQueue",
            BasicTypeInfo.STRING_TYPE_INFO,
            TypeInformation.of(new TypeHint<Queue<Resource>>() {
            }));
    private transient MapState<String, Map<String, Resource>> viewState;
    private MapStateDescriptor<String, Map<String, Resource>> viewStateDescriptor = new MapStateDescriptor<>(
            "viewState",
            BasicTypeInfo.STRING_TYPE_INFO,
            TypeInformation.of(new TypeHint<Map<String, Resource>>() {
            }));

    public static final OutputTag<Resource> RESOURCE_OUTPUT_TAG = new OutputTag<Resource>(
            "resourceOutputTag") {
    };

    @Override
    public void open(Configuration parameters) {
        this.resourceQueues = getRuntimeContext().getMapState(resourceQueueDescriptor);
        this.viewState = getRuntimeContext().getMapState(viewStateDescriptor);
    }

    @Override
    public void processElement(EnrichedEvent event,
            KeyedProcessFunction<String, EnrichedEvent, CorrelatedEvent>.Context ctx, Collector<CorrelatedEvent> out)
            throws Exception {

        List<OcelRelationship> correlations = new ArrayList<>();
        
        // Default correlations, correlating device Id, machine Id, etc.
        String equipmentId = event.getEvent().getRule().getEquipmentId();
        String equipmentPath = event.getEvent().getRule().getEquipmentPath();
        correlations.add(new OcelRelationship(equipmentId, "equipmentId"));
        correlations.add(new OcelRelationship(equipmentPath, "equipmentPath"));

        // Update view state
        String viewId = event.getEvent().getRule().getViewId();
        if (viewId != null) {
            Map<String, Resource> views = this.viewState.get(equipmentPath);
            if (views == null) {
                views = new HashMap<>();
            }
            String resourceId = UUID.randomUUID().toString();
            Resource viewCorrelation = new Resource(resourceId, viewId + event.getEvent().getRule().getEventName(), event.getEnrichment());
            views.put(viewId, viewCorrelation);
            this.viewState.put(equipmentPath, views);
        }
        // Correlate views along equipmentPath
        String[] equipment = equipmentPath.split(",");

        for (int i = 0; i < equipment.length; i++) {
            String currentPath = String.join(",", java.util.Arrays.copyOfRange(equipment, 0, i)) + ",";
            Map<String, Resource> views = this.viewState.get(currentPath);
            if (views != null) {
                for (Resource resource : views.values()) {
                    correlations.add(new OcelRelationship(resource.getResourceId(), resource.getResourceModelId()));
                }
            }
        }

        // Correlate according to correlation rules
        List<EventResourceRelation> correlationRules = event.getEvent().getRule().getRelations();
        for (EventResourceRelation correlationRule : correlationRules) {
            Queue<Resource> resourceQueue = this.resourceQueues.get(correlationRule.getResourceModelId());
            if (resourceQueue == null) {
                resourceQueue = new LinkedList<>();
            }
            Resource resource;
            switch (correlationRule.getInteractionType()) {
                case "CREATE":
                    String resourceId = UUID.randomUUID().toString();
                    correlations.add(new OcelRelationship(correlationRule.getQualifier(), resourceId));
                    resource = new Resource(resourceId, correlationRule.getResourceModelId(), event.getEnrichment());
                    resourceQueue.add(resource);
                    break;
                case "CONSUME":
                    resource = resourceQueue.poll();
                    if (resource == null) {
                        // TODO think about what to do here
                        continue;
                    }
                    correlations.add(new OcelRelationship(correlationRule.getQualifier(), resource.getResourceId()));
                    break;
                default:
                    continue;
            }
            resourceQueues.put(correlationRule.getResourceModelId(), resourceQueue);
            ctx.output(RESOURCE_OUTPUT_TAG, resource);
        }
        out.collect(new CorrelatedEvent(event.getEvent(), event.getEnrichment(), correlations));
    }
}
