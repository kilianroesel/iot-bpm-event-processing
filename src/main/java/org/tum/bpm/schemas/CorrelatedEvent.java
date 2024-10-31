package org.tum.bpm.schemas;

import java.util.List;

import org.tum.bpm.schemas.ocel.OcelAttribute;
import org.tum.bpm.schemas.ocel.OcelRelationship;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CorrelatedEvent {

    private final BaseEvent event;
    private List<OcelAttribute> enrichment;
    private List<OcelRelationship> correlation;

}
