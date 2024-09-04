package org.tum.ocel;

import java.util.List;

public class OcelEventType {
    private final String name;
    private List<AttributeType> attributeTypes;

    public OcelEventType(String name) {
        this.name = name;
    }

    public OcelEventType(String name, List<AttributeType> attributeTypes) {
        this.name = name;
        this.attributeTypes = attributeTypes;
    }

    public String getName() {
        return this.name;
    }

    public List<AttributeType> getAttributes() {
        return this.attributeTypes;
    }
}
