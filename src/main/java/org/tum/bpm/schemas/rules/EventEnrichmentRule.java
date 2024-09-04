package org.tum.bpm.schemas.rules;

import org.apache.flink.cdc.connectors.shaded.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.cdc.connectors.shaded.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.cdc.connectors.shaded.com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"id", "name", "field", "equipmentId"})
public class EventEnrichmentRule {
    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("field")
    private String field;
    @JsonProperty("equipmentId")
    private String equipmentId;
}
