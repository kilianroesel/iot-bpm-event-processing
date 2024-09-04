package org.tum.bpm.schemas.measurements;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"time", "value", "field", "deviceId", "machineId", "machineName"})
public class CSVMeasurement implements Comparable<CSVMeasurement> {
    @JsonProperty("time")
    private ZonedDateTime time;

    @JsonProperty("value")
    private String value;

    @JsonProperty("field")
    private String field;

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("machineId")
    private String machineId;

    @JsonProperty("machineName")
    private String machineName;

    @Override
    public int compareTo(CSVMeasurement o) {
        return time.compareTo(o.getTime());
    }
}
