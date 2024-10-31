package org.tum.bpm.schemas.debezium;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "_id", "operationType", "fullDocument", "ns" })
public class MongoChangeStreamMessage {

    @JsonProperty("_id")
    private String id;

    @JsonProperty("operationType")
    private String operationType;

    @JsonProperty("fullDocument")
    private String fullDocument;

    @JsonProperty("ns")
    private Namespace namespace;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public class Namespace {
        @JsonProperty("db")
        private String db;

        @JsonProperty("coll")
        private String collection;
    }
}
