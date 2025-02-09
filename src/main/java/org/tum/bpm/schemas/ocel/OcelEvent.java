package org.tum.bpm.schemas.ocel;

import java.time.Instant;
import java.util.List;

import org.tum.bpm.sinks.dynamicMongoSink.DynamicMongoDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcelEvent implements DynamicMongoDocument {
    private String id;
    private String type;
    private Instant time;
    private List<OcelAttribute> attributes;
    private List<OcelRelationship> relationships;

    public String getCollection() {
        return "hallo";
    }
}
