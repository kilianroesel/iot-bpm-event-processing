package org.tum.bpm.schemas.rules;

import java.time.Instant;

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
    use = JsonTypeInfo.Id.DEDUCTION,
    include = JsonTypeInfo.As.EXISTING_PROPERTY
)
@JsonSubTypes({
    @Type(EventAbstractionRule.class),
    @Type(EventEnrichmentRule.class),
    @Type(EventScopingRule.class),
})
@JsonPropertyOrder({"id", "updatedAt", "createdAt"})
public class Rule {
 
    @JsonProperty("id")
    private String ruleId;
    @JsonProperty("updatedAt")
    private Instant updatedAt;
    @JsonProperty("createdAt")
    private Instant createdAt;
}
