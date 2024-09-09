package org.tum.bpm.schemas.rules;

import java.time.ZonedDateTime;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "__t"
)
@JsonSubTypes({
    @Type(EventAbstractionRule.class),
    @Type(EventEnrichmentRule.class),
    @Type(EventScopingRule.class),
})
@JsonPropertyOrder({"_id", "__t", "control", "updatedAt", "createdAt"})
public class Rule {
 
    @JsonProperty("_id")
    private String _id;
    @JsonProperty("__t")
    private String __t;
    @JsonProperty("updatedAt")
    private ZonedDateTime updatedAt;
    @JsonProperty("createdAt")
    private ZonedDateTime createdAt;
    @JsonProperty("control")
    private Control control;

    public String getId() {
        return this._id;
    }

    public enum Control {
        ACTIVE,
        INACTIVE
    }
}
