package org.tum.bpm.schemas;

import java.util.Map;

import org.tum.bpm.schemas.measurements.IoTMessageSchema;
// import org.tum.bpm.schemas.rules.EventAbstractionRule;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AttributeEvent<T extends IoTMessageSchema> {

    private final String id;
    private final String name;
    // The rule that caused the event
    // private final EventAbstractionRule rule;
    // The event that triggered the event
    // private final T trigger;'
    // Additional relevant event information
    private final Map<String, T> attributes;
}
