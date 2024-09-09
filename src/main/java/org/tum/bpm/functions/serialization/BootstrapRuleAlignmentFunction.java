package org.tum.bpm.functions.serialization;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.jsontype.NamedType;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.tum.bpm.schemas.rules.EventAbstractionRule;

public class BootstrapRuleAlignmentFunction extends ProcessFunction<String, String> {
    private transient ObjectMapper objectMapper;

    @Override
    public void open(Configuration parameters) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.registerSubtypes(new NamedType(EventAbstractionRule.class, "EventAbstractionRule"));
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void processElement(String value, ProcessFunction<String, String>.Context ctx, Collector<String> out)
            throws Exception {

        JsonNode jsonNode = objectMapper.readTree(value);
        ((ObjectNode) jsonNode).put("_id", jsonNode.get("_id").get("$oid").asText());
        ((ObjectNode) jsonNode).put("updatedAt", jsonNode.get("updatedAt").get("$date").asText());
        ((ObjectNode) jsonNode).put("createdAt", jsonNode.get("createdAt").get("$date").asText());

        String transformedJson = objectMapper.writeValueAsString(jsonNode);
        out.collect(transformedJson);
    }

}