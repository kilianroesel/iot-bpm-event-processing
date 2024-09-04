package org.tum.ocel;

public class Attribute {
    private AttributeType attributeType;
    private String value;

    public Attribute(AttributeType attributeType, String value) {
        this.attributeType = attributeType;
        this.value = value;
    }

    public String geValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public AttributeType getAttributeType() {
        return attributeType;
    }

    @Override
    public String toString() {
        return "Attribute [name=" + attributeType.getName() + ", type=" + attributeType.getType() + ", value=" + this.geValue() + "]";
    }
}