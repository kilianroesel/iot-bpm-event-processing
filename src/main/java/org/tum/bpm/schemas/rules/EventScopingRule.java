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
@JsonPropertyOrder({"id", "createdAt", "updatedAt", "machineName", "versionCsiStd", "versionCsiSpecific"})
public class EventScopingRule extends Rule {
    @JsonProperty("machineName")
    private String machineName;
    @JsonProperty("versionCsiStd")
    private String versionCsiStd;
    @JsonProperty("versionCsiSpecific")
    private String versionCsiSpecific;

    public EventScopingRule() {
        super();
    }
}
