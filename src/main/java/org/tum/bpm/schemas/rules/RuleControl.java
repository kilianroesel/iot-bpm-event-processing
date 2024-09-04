package org.tum.bpm.schemas.rules;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RuleControl<RULE> {
    private final RULE rule;
    private final Control control;

    public enum Control {
        ACTIVE,
        INACTIVE
    }
}
