package org.tum.bpm.schemas;

import java.util.List;

import org.tum.bpm.schemas.ocel.OcelAttribute;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EnrichedEvent {

    private final BaseEvent event;
    private final List<OcelAttribute> enrichment;
}
