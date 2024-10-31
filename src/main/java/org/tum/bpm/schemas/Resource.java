package org.tum.bpm.schemas;

import java.util.List;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.tum.bpm.schemas.ocel.OcelAttribute;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Resource {
    
    @JsonProperty("resourceId")
    private final String resourceId;

    @JsonProperty("resourceModelId")
    private final String resourceModelId;

    @JsonProperty("enrichment")
    private final List<OcelAttribute> enrichment;
}
