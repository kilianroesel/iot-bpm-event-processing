package org.tum.bpm.schemas.measurements;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.Instant;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "edgeDeviceId", "versionCsiStd",
        "machineMasterSoftwareVersion", "varName", "varDataType", "varValue", "machineType", "machineNumber",
        "machineName", "machineId", "timestampUtc", "timestampMachine" })
public class CSIMeasurement implements Comparable<CSIMeasurement>  {

    @JsonProperty("edgeDeviceId")
    private String edgeDeviceId;

    @JsonProperty("versionCsiStd")
    private String versionCsiStd;

    @JsonProperty("versionCsiSpecific")
    private String versionCsiSpecific;

    @JsonProperty("varName")
    private String varName;

    @JsonProperty("varDataType")
    private String varDataType;

    @JsonProperty("varValue")
    private String varValue;

    @JsonProperty("machineType")
    private String machineType;

    @JsonProperty("machineNumber")
    private String machineNumber;

    @JsonProperty("machineName")
    private String machineName;

    @JsonProperty("machineId")
    private String machineId;

    @JsonProperty("timestampUtc")
    private Instant timestampUtc;

    @JsonProperty("timestampMachine")
    private LocalDateTime timestampMachine;

    @Override
    public int compareTo(CSIMeasurement o) {
        return this.timestampUtc.compareTo(o.getTimestampUtc());
    }
}