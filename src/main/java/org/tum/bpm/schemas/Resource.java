package org.tum.bpm.schemas;

import java.time.Instant;
import java.util.List;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.tum.bpm.schemas.ocel.OcelAttribute;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Resource {
    
    @JsonProperty("resourceId")
    private String resourceId;

    @JsonProperty("resourceModelId")
    private String resourceModelId;

    @JsonProperty("enrichment")
    private List<OcelAttribute> enrichment;

    // After this watermark the resource is not valid anymore
    @JsonProperty("maxTimestamp")
    private Instant maxTimestamp;
}
