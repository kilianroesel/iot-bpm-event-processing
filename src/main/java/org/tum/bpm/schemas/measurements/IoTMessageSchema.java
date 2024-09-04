package org.tum.bpm.schemas.measurements;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({ "schemaVersion", "payloadType", "messageId", "correlationId",
        "messageTs", "payload" })
public class IoTMessageSchema implements Comparable<IoTMessageSchema> {
    @JsonProperty("schemaVersion")
    private String schemaVersion;

    @JsonProperty("payloadType")
    private String payloadType;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("messageTs")
    private ZonedDateTime messageTs;
    
    @JsonProperty("payload")
    private CSIMeasurement payload;

    @Override
    public int compareTo(IoTMessageSchema o) {
        return this.payload.compareTo(o.getPayload());
    }
}