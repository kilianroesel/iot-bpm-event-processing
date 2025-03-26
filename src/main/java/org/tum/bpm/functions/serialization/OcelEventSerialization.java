package org.tum.bpm.functions.serialization;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.apache.flink.api.common.functions.MapFunction;
import org.tum.bpm.schemas.CorrelatedEvent;
import org.tum.bpm.schemas.ocel.OcelAttribute;
import org.tum.bpm.schemas.ocel.OcelEvent;
import org.tum.bpm.schemas.ocel.OcelRelationship;
import org.tum.bpm.sinks.dynamicMongoSink.MetaDocument;

public class OcelEventSerialization implements MapFunction<CorrelatedEvent, MetaDocument<OcelEvent>> {

    @Override
    public MetaDocument<OcelEvent> map(CorrelatedEvent event) throws Exception {
        String eventId = UUID.randomUUID().toString();
        String eventName =  event.getEvent().getRule().getEventName();
        Instant eventTime = event.getEvent().getIotMessage().getPayload().getTimestampUtc();
        Instant sendTime = event.getEvent().getIotMessage().getMessageTs();
        Instant ingestionTime = event.getEvent().getIotMessage().getIngestionTime();
        Instant scopeTime = event.getEvent().getScopeTime();
        Instant abstractionTime = event.getEvent().getAbstractionTime();
        Instant enrichmentTime = event.getEnrichmentTime();
        Instant correlationTime = event.getCorrelationTime();
        List<OcelAttribute> eventAttributes = event.getEnrichment();
        List<OcelRelationship> ocelRelationships = event.getCorrelation();

        String collection = event.getEvent().getIotMessage().getPayload().getEdgeDeviceId();

        OcelEvent ocelEvent = new OcelEvent(eventId, eventName,
        eventTime, eventAttributes,
        ocelRelationships);

        MetaDocument<OcelEvent> wrappedOcelEvent = new MetaDocument<>(collection, eventTime, sendTime, ingestionTime, ocelEvent, scopeTime, abstractionTime, enrichmentTime, correlationTime);
        return wrappedOcelEvent; 
    }
}
