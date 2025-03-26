package org.tum.bpm.schemas.measurements;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@JsonPropertyOrder({ "schemaVersion", "payloadType", "messageId", "correlationId",
        "messageTs", "payload" })
public class IoTMessageSchema implements Comparable<IoTMessageSchema> {

    public IoTMessageSchema() {
        this.ingestionTime = Instant.now(); // Capture deserialization time
    }

    @JsonProperty("schemaVersion")
    private String schemaVersion;

    @JsonProperty("payloadType")
    private String payloadType;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("messageTs")  // When was the message created on the edge
    private Instant messageTs;
    
    @JsonProperty("payload")
    private CSIMeasurement payload;

    private Instant ingestionTime;


    @Override
    public int compareTo(IoTMessageSchema o) {
        return this.payload.compareTo(o.getPayload());
    }
}