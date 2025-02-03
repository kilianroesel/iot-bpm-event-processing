package org.tum.bpm.schemas;

import java.util.List;

import org.tum.bpm.schemas.ocel.OcelAttribute;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EnrichedEvent {

    private BaseEvent event;
    private List<OcelAttribute> enrichment;
}
