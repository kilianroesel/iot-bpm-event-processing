package org.tum.bpm.functions.correlation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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

    public static final OutputTag<Resource> RESOURCE_OUTPUT_TAG = new OutputTag<Resource>(
            "resourceOutputTag") {
    };

    @Override
    public void open(Configuration parameters) {
        this.resourceQueues = getRuntimeContext().getMapState(resourceQueueDescriptor);
    }

    @Override
    public void processElement(EnrichedEvent event,
            KeyedProcessFunction<String, EnrichedEvent, CorrelatedEvent>.Context ctx, Collector<CorrelatedEvent> out)
            throws Exception {
        
        List<EventResourceRelation> correlationRules = event.getEvent().getRule().getRelations();
        List<OcelRelationship> correlations = new ArrayList<>();
        for (EventResourceRelation correlationRule: correlationRules) {
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
                        //TODO think about what to do here
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
