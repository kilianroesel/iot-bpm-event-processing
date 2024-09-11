package org.tum.bpm.schemas.debezium;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;


@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebeziumMessageSource {
    /**
     * Debezium version
     */
    @JsonProperty("version")
    private String version;
    /**
     * Connector type
     */
    @JsonProperty("connector")
    private String connector;

    /**
     * Connector name
     */
    @JsonProperty("name")
    private String name;

    /** 
     * If the event was part of a snapshot
    */
    @JsonProperty("snapshot")
    private boolean snapshot;
    /**
     * Database name
     */
    @JsonProperty("db")
    private String db;

    /**
     * Schema name
     */
    @JsonProperty("schema")
    private String schema;

    /**
     * Table name
     */
    @JsonProperty("table")
    private String table;

    /**
     * Transaction Id
     */
    @JsonProperty("txId")
    private Integer txId;

    /**
     * Timestamp when the change was made in the db
     */
    @JsonProperty("ts_ms")
    private Instant ts_ms;

}
