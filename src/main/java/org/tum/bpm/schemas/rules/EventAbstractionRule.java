package org.tum.bpm.schemas.rules;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=true)
@JsonPropertyOrder({"id", "createdAt", "updatedAt", "eventName", "field", "triggerCategory", "triggerType", "value", "from", "to", "scopeId","equipmentId", "equipmentPath", "viewId", "relations"})
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
    @JsonProperty("equipmentPath")
    private String equipmentPath;
    @JsonProperty("viewId")
    private String viewId;
    @JsonProperty("relations")
    private List<EventResourceRelation> relations;

    public EventAbstractionRule() {
        super();
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder({"resourceModelId", "interactionType", "qualifier"})
    public static class EventResourceRelation {

        @JsonProperty("resourceModelId")
        private String resourceModelId;

        @JsonProperty("interactionType")
        private String interactionType;

        @JsonProperty("qualifier")
        private String qualifier;
    }
}
