package org.tum.bpm.schemas.rules;


import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper=true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"id", "createdAt", "updatedAt", "statusName", "field", "equipmentId", "viewId"})
public class EventEnrichmentRule extends Rule {
    @JsonProperty("statusName")
    private String statusName;
    @JsonProperty("field")
    private String field;
    @JsonProperty("equipmentId")
    private String equipmentId;
    @JsonProperty("viewId")
    private String viewId;

    public EventEnrichmentRule() {
        super();
    }
}
