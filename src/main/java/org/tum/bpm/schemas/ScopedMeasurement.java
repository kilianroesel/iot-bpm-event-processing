package org.tum.bpm.schemas;

import java.time.Instant;

import org.tum.bpm.schemas.measurements.IoTMessageSchema;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScopedMeasurement {
  private IoTMessageSchema iotMessage;
  private String scope;
  private Instant ingestionTime;
  private Instant scopeTime;

  public ScopedMeasurement(IoTMessageSchema iotMessage, String scope, Instant ingestionTime) {
    this.scopeTime = Instant.now();
    this.iotMessage = iotMessage;
    this.scope = scope;
    this.ingestionTime = ingestionTime;
  }
}
