package org.tum.ocel;

import java.util.List;

public class OcelObjectType {
    private final String name;
    private List<AttributeType> attributeTypes;

    public OcelObjectType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void addAttributeType(AttributeType attributeType) {
        this.attributeTypes.add(attributeType);
    }
}
