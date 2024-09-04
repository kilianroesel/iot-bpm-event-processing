package org.tum.ocel;

import java.util.List;

public class OcelObject {
    private String id;
    private String name;
    private List<Attribute> attributes;

    public String getId() {
        return this.id;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "OcelObject [name=" + name + ", attributes=" + attributes + "]";
    }
}
