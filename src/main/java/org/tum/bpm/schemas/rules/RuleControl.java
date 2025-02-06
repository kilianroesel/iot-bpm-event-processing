package org.tum.bpm.schemas.rules;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleControl<T extends Rule> {
    private T rule;
    private Control control;

    public enum Control {
        ACTIVE,
        INACTIVE
    }
}
