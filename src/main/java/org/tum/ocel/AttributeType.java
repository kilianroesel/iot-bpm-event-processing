package org.tum.ocel;

public class AttributeType {
    private String name;
    private String type;

    public AttributeType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Attribute [name=" + name + ", type=" + type + "]";
    }
}
