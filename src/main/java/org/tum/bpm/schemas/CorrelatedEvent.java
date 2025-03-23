package org.tum.bpm.schemas;

import java.time.Instant;
import java.util.List;

import org.tum.bpm.schemas.ocel.OcelAttribute;
import org.tum.bpm.schemas.ocel.OcelRelationship;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CorrelatedEvent {

    private BaseEvent event;
    private List<OcelAttribute> enrichment;
    private List<OcelRelationship> correlation;
    private Instant abstractionTime;
    private Instant enrichmentTime;
    private Instant correlationTime;

    public CorrelatedEvent(BaseEvent event, List<OcelAttribute> enrichment, List<OcelRelationship> correlation, Instant eventAbstractionTime, Instant eventEnrichmentTime) {
        this.correlationTime = Instant.now();
        this.event = event;
        this.enrichment = enrichment;
        this.correlation = correlation;
        this.abstractionTime = eventAbstractionTime;
        this.enrichmentTime = eventEnrichmentTime;
    }

}
