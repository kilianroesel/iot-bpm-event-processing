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
@JsonPropertyOrder({"id", "machineName", "versionCsiStd", "versionCsiSpecific", "machineSoftwareVersion", "machineMasterSoftwareVersion"})
public class EventScopingRule {
    @JsonProperty("id")
    private String id;
    @JsonProperty("machineName")
    private String machineName;
    @JsonProperty("versionCsiStd")
    private String versionCsiStd;
    @JsonProperty("versionCsiSpecific")
    private String versionCsiSpecific;
    @JsonProperty("machineSoftwareVersion")
    private String machineSoftwareVersion;
    @JsonProperty("machineMasterSoftwareVersion")
    private String machineMasterSoftwareVersion;
}
