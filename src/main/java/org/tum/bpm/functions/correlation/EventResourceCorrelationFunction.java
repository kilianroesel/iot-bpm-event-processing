package org.tum.bpm.functions.correlation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import org.tum.bpm.schemas.stats.Alarm;

public class EventResourceCorrelationFunction extends KeyedProcessFunction<String, EnrichedEvent, CorrelatedEvent> {

    private transient MapState<String, Queue<Resource>> resourceQueues;
    private MapStateDescriptor<String, Queue<Resource>> resourceQueueDescriptor = new MapStateDescriptor<>(
            "resourceQueue",
            BasicTypeInfo.STRING_TYPE_INFO,
            TypeInformation.of(new TypeHint<Queue<Resource>>() {
            }));
    // Maps equipmentPath to view to view state
    private transient MapState<String, Map<String, Resource>> viewState;
    private MapStateDescriptor<String, Map<String, Resource>> viewStateDescriptor = new MapStateDescriptor<>(
            "viewState",
            BasicTypeInfo.STRING_TYPE_INFO,
            TypeInformation.of(new TypeHint<Map<String, Resource>>() {
            }));

    public static final OutputTag<Resource> RESOURCE_OUTPUT_TAG = new OutputTag<Resource>(
            "resourceOutputTag") {
    };

    public static final OutputTag<Alarm> ALARM_OUTPUT_TAG = new OutputTag<Alarm>(
            "alarmOutputTag") {
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
        String deviceId = event.getEvent().getIotMessage().getPayload().getEdgeDeviceId();
        String machineId = event.getEvent().getRule().getScopeId();
        String equipmentId = event.getEvent().getRule().getEquipmentId();
        String equipmentPath = event.getEvent().getRule().getEquipmentPath();
        String viewId = event.getEvent().getRule().getViewId();

        correlations.add(new OcelRelationship(deviceId, "lineId"));
        correlations.add(new OcelRelationship(equipmentId, "equipmentId"));
        correlations.add(new OcelRelationship(machineId, "machineId"));
        correlations.add(new OcelRelationship(equipmentPath, "equipmentPath"));
        correlations.add(new OcelRelationship(viewId, "viewId"));

        // Update view state
        if (viewId != null) {
            Map<String, Resource> views = this.viewState.get(equipmentPath);
            if (views == null) {
                views = new HashMap<>();
            }
            String resourceId = UUID.randomUUID().toString();
            Resource viewCorrelation = new Resource(resourceId, viewId + event.getEvent().getRule().getEventName(),
                    event.getEnrichment(), Instant.MAX);
            views.put(viewId, viewCorrelation);
            this.viewState.put(equipmentPath, views);
        }
        // Correlate views along equipmentPath
        String[] equipment = equipmentPath.split(",");

        for (int i = 1; i < equipment.length; i++) {
            String currentPath = String.join(",", java.util.Arrays.copyOfRange(equipment, 0, i + 1)) + ",";
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
            String resourceId;
            String edgeDeviceId = event.getEvent().getIotMessage().getPayload().getEdgeDeviceId();
            Instant eventTime = event.getEvent().getIotMessage().getPayload().getTimestampUtc();
            Instant validity = Instant.MAX;
            if (correlationRule.getLifespan() != null) {
                validity = eventTime.plusSeconds(correlationRule.getLifespan());
            }

            // Remove expired resources
            LinkedList<Resource> linkedList = (LinkedList<Resource>) resourceQueue;
            Iterator<Resource> iterator = linkedList.iterator();
            while (iterator.hasNext()) {
                Resource expResource = iterator.next();
                if (eventTime.isAfter(expResource.getMaxTimestamp())) {
                    iterator.remove();
                    ctx.output(ALARM_OUTPUT_TAG,
                            new Alarm("Resource is expired", "Resource is expired: (edgeDeviceId: " + edgeDeviceId +  ", resourceModelId: " + expResource.getResourceModelId() + ")", eventTime));
                    continue;
                }
            }

            switch (correlationRule.getInteractionType()) {
                case "CREATE":
                    if (correlationRule.getQuantity() != null) {
                        for (int i = 0; i < correlationRule.getQuantity(); i++) {
                            resourceId = UUID.randomUUID().toString();
                            correlations.add(new OcelRelationship(resourceId, correlationRule.getResourceModelId()));
                            resource = new Resource(resourceId, correlationRule.getResourceModelId(),
                                    event.getEnrichment(), validity);
                            resourceQueue.add(resource);
                            ctx.output(RESOURCE_OUTPUT_TAG, resource);
                        }
                    }
                    break;
                case "PROVIDE":
                    resourceId = UUID.randomUUID().toString();
                    correlations.add(new OcelRelationship(resourceId, correlationRule.getResourceModelId()));
                    resource = new Resource(resourceId, correlationRule.getResourceModelId(), event.getEnrichment(),
                            validity);
                    resourceQueue.clear();
                    resourceQueue.add(resource);
                    ctx.output(RESOURCE_OUTPUT_TAG, resource);
                    break;
                case "REFERENCE":
                    if (correlationRule.getQuantity() != null) {
                        for (int i = 0; i < correlationRule.getQuantity(); i++) {
                            String referencedModelId = correlationRule.getReferenceModelId();
                            if (referencedModelId == null) {
                                ctx.output(ALARM_OUTPUT_TAG,
                                        new Alarm("Could not reference resource. Rule does not contain reference modelId.",
                                        "Could not reference resource. Rule does not contain reference modelId",
                                        eventTime));
                                continue;
                            }
                            Queue<Resource> referencedResourceQueue = this.resourceQueues.get(referencedModelId);
                            if (referencedResourceQueue == null) {
                                ctx.output(ALARM_OUTPUT_TAG,
                                        new Alarm("Could not reference resource. ResourceQueue does not yet exist.",
                                        "Could not reference resource. ResourceQueue does not yet exist: (edgeDeviceId: " + edgeDeviceId +  ", resourceModelId: " + correlationRule.getResourceModelId() + ")",
                                        eventTime));
                                continue;
                            }

                            resource = referencedResourceQueue.poll();
                            if (resource == null) {
                                ctx.output(ALARM_OUTPUT_TAG,
                                        new Alarm("Could not reference resource. No resource available.",
                                        "Could not reference resource. No resource available: (edgeDeviceId: " + edgeDeviceId +  ", resourceModelId: " + correlationRule.getResourceModelId() + ")",
                                        eventTime));
                                continue;
                            }
                            correlations.add(
                                    new OcelRelationship(resource.getResourceId(), resource.getResourceModelId()));
                            resourceQueue.add(resource);
                            ctx.output(RESOURCE_OUTPUT_TAG, resource);
                        }
                    }
                    break;
                case "CONSUME":
                    if (correlationRule.getQuantity() != null) {
                        for (int i = 0; i < correlationRule.getQuantity(); i++) {
                            resource = resourceQueue.poll();
                            if (resource == null) {
                                ctx.output(ALARM_OUTPUT_TAG,
                                        new Alarm("Could not consume resource. No resource available.",
                                        "Could not consume resource. No resource available: (edgeDeviceId: " + edgeDeviceId +  ", resourceModelId: " + correlationRule.getResourceModelId() + ")",
                                        eventTime));
                                continue;
                            }
                            correlations.add(
                                    new OcelRelationship(resource.getResourceId(), correlationRule.getResourceModelId()));
                        }
                    }
                    break;
                case "USE": // Does not remove the resource from the queue
                    resource = resourceQueue.peek();
                    if (resource == null) {
                        ctx.output(ALARM_OUTPUT_TAG,
                                        new Alarm("Could not use resource. No resource available.",
                                        "Could not use resource. No resource available: (edgeDeviceId: " + edgeDeviceId +  ", resourceModelId: " + correlationRule.getResourceModelId() + ")",
                                        eventTime));
                        continue;
                    }
                    correlations.add(new OcelRelationship(resource.getResourceId(), correlationRule.getResourceModelId()));
                default:
                    continue;
            }
            resourceQueues.put(correlationRule.getResourceModelId(), resourceQueue);
        }
        out.collect(new CorrelatedEvent(event.getEvent(), event.getEnrichment(), correlations, event.getEventAbstractionTime(), event.getEventEnrichmentTime()));
    }
}
