package org.tum.bpm.schemas;

import java.time.ZonedDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AttributeEvent<T> {

    private final String id;
    private final String name;
    private final ZonedDateTime time;
    // The rule that caused the event
    // private final EventAbstractionRule rule;
    // The event that triggered the event
    // private final T trigger;'
    // Additional relevant event information
    private final Map<String, T> attributes;
}
