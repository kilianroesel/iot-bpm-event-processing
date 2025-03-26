package org.tum.bpm.schemas;

import java.time.Instant;
import java.util.List;

import org.tum.bpm.schemas.ocel.OcelAttribute;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EnrichedEvent {

    private AbstractedEvent event;
    private List<OcelAttribute> enrichment;
    private Instant eventEnrichmentTime;


    public EnrichedEvent(AbstractedEvent event, List<OcelAttribute> enrichment) {
        this.event = event;
        this.enrichment = enrichment;
        this.eventEnrichmentTime = Instant.now();
    }
}
