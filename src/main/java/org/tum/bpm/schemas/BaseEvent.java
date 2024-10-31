package org.tum.bpm.schemas;

import org.tum.bpm.schemas.measurements.IoTMessageSchema;
import org.tum.bpm.schemas.rules.EventAbstractionRule;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BaseEvent {
    // The rule that triggered the event
    private final EventAbstractionRule rule;
    // The underlying messsage that the event was evaluated on
    private final IoTMessageSchema iotMessage;
}
