package org.tum.bpm.schemas;

import java.util.List;

import org.tum.bpm.schemas.rules.EventEnrichmentRule;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EquipmentListEvent {
    private final BaseEvent baseEvent;
    private final List<EventEnrichmentRule> enrichmentRules;
}
