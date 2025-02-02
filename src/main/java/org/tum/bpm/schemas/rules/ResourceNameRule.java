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
@JsonPropertyOrder({"id", "createdAt", "updatedAt", "resourceId"})
public class ResourceNameRule extends Rule {
    @JsonProperty("resourceModelName")
    private String resourceModelName;

    public ResourceNameRule() {
        super();
    }
}
