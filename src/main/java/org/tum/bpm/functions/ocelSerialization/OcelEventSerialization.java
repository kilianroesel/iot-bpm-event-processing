package org.tum.bpm.functions.ocelSerialization;

import java.util.UUID;

import org.apache.flink.api.common.functions.MapFunction;
import org.tum.bpm.schemas.CorrelatedEvent;
import org.tum.bpm.schemas.ocel.OcelEvent;

public class OcelEventSerialization implements MapFunction<CorrelatedEvent, OcelEvent> {

    @Override
    public OcelEvent map(CorrelatedEvent event) throws Exception {
        String eventId = UUID.randomUUID().toString();
        return new OcelEvent(eventId, event.getEvent().getRule().getEventName(), event.getEvent().getIotMessage().getPayload().getTimestampUtc(), event.getEnrichment(), event.getCorrelation());
    }
    
}
