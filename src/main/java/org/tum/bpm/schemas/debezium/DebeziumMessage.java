package org.tum.bpm.schemas.debezium;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "before", "after", "source", "ts_ms", "op" })
public class DebeziumMessage<T> {
    
    @JsonProperty("before")
    private T before;

    @JsonProperty("after")
    private T after;

    @JsonProperty("source")
    private DebeziumMessageSource source;

    /**
     * Optional field describing when connector processed the event
     */
    @JsonProperty("ts_ms")
    private Instant ts_ms;

    /**
     * Mandatory string that describes the type of operation.
     */
    @JsonProperty("op")
    private String op;
}
