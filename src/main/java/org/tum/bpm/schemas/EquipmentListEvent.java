package org.tum.bpm.schemas;

import java.util.List;

import org.tum.bpm.schemas.measurements.IoTMessageSchema;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EquipmentListEvent<T extends IoTMessageSchema> {
    private final BaseEvent<T> baseEvent;
    private final List<String> statusFields;
}
