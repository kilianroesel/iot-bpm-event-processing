package org.tum.bpm.schemas;

import java.time.Instant;
import java.util.List;

import org.tum.bpm.schemas.ocel.OcelAttribute;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EnrichedEvent {

    private BaseEvent event;
    private List<OcelAttribute> enrichment;
    private Instant eventAbstractionTime;
    private Instant eventEnrichmentTime;


    public EnrichedEvent(BaseEvent event, List<OcelAttribute> enrichment, Instant eventAbstractionTime) {
        this.event = event;
        this.enrichment = enrichment;
        this.eventAbstractionTime = eventAbstractionTime;
        this.eventEnrichmentTime = Instant.now();
    }
}
