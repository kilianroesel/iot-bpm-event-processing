package org.tum.bpm.schemas.rules;

import org.apache.flink.cdc.connectors.shaded.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.cdc.connectors.shaded.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.cdc.connectors.shaded.com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=true)
@JsonPropertyOrder({"__t", "eventName", "field", "triggerCategory", "triggerType", "value", "equipmentId", "control", "scopeId", "updatedAt", "createdAt"})
public class EventAbstractionRule extends Rule {
    @JsonProperty("eventName")
    private String eventName;    
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
    @JsonProperty("scopeId")
    private String scopeId;
    @JsonProperty("equipmentId")
    private String equipmentId;

    public EventAbstractionRule() {
        super();
    }
}
