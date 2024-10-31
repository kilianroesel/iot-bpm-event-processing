package org.tum.bpm.schemas.rules;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RuleControl<T extends Rule> {
    private final T rule;
    private final Control control;

    public enum Control {
        ACTIVE,
        INACTIVE
    }
}
