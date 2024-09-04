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
@JsonPropertyOrder({"id", "name", "field", "triggerCategory", "triggerType", "value", "from", "to", "equipmentId", "scopeId"})
public class EventAbstractionRule {
    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("field")
    private String field;
    @JsonProperty("triggerCategory")
    private String triggerCategory;
    @JsonProperty("triggerType")
    private String triggerType;
    @JsonProperty("value")
    private Double value;
    @JsonProperty("from")
    private Double from;
    @JsonProperty("to")
    private Double to;
    @JsonProperty("equipmentId")
    private String equipmentId;
    @JsonProperty("scopeId")
    private String scopeId;
}
