package org.tum.bpm.schemas;

import java.time.Instant;

import org.tum.bpm.schemas.measurements.IoTMessageSchema;
import org.tum.bpm.schemas.rules.EventAbstractionRule;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AbstractedEvent {
    // The rule that triggered the event
    private EventAbstractionRule rule;
    // The underlying messsage that the event was evaluated on
    private IoTMessageSchema iotMessage;
    private Instant scopeTime;
    private Instant abstractionTime;

    public AbstractedEvent(EventAbstractionRule rule, IoTMessageSchema iotMessage, Instant scopeTime) {
        this.abstractionTime = Instant.now();
        this.scopeTime = scopeTime;
        this.iotMessage = iotMessage;
        this.rule = rule;
    }
}
