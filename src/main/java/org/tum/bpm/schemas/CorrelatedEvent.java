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

    private AbstractedEvent event;
    private List<OcelAttribute> enrichment;
    private List<OcelRelationship> correlation;
    private Instant enrichmentTime;
    private Instant correlationTime;

    public CorrelatedEvent(AbstractedEvent event, List<OcelAttribute> enrichment, List<OcelRelationship> correlation, Instant enrichmentTime) {
        this.correlationTime = Instant.now();
        this.event = event;
        this.enrichment = enrichment;
        this.correlation = correlation;
        this.enrichmentTime = enrichmentTime;
    }

}
