package org.tum.bpm.schemas.rules;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=true)
@JsonPropertyOrder({"id", "createdAt", "updatedAt", "eventId", "equipmentId", "lifecycleId", "qualifier", "objectModelId", "objectInteractionType"})
public class ResourceEnrichmentRule extends Rule {
    @JsonProperty("eventId")
    private String eventId;    
    @JsonProperty("equipmentId")
    private String equipmentId;
    @JsonProperty("lifecycleId")
    private String lifecycleId;
    @JsonProperty("qualifier")
    private String qualifier;

    @JsonProperty("objectModelId")
    private Double objectModelId;
    @JsonProperty("objectInteractionType")
    private Double objectInteractionType;

    public ResourceEnrichmentRule() {
        super();
    }
}
