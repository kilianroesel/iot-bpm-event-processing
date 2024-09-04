package org.tum.ocel;

import java.util.List;

public class OcelEvent {
    private final String id;
    private final String time;
    private OcelEventType ocelEventType;
    private List<Attribute> attributes;
    private List<Relationship> relationships;

    
    public OcelEvent(String id, String time, OcelEventType ocelEventType) {
        this.id = id;
        this.time = time;
        this.ocelEventType = ocelEventType;
    }

    public String getId() {
        return this.id;
    }

    public String getTime() {
        return this.time;
    }

    public String getName() {
        return this.ocelEventType.getName();
    }

    public OcelEventType getEventType() {
        return this.ocelEventType;
    }

    public List<Attribute> getAttributes() {
        return this.attributes;
    }

    public void addAttribute(Attribute attribute) {
        this.attributes.add(attribute);
    }

    public List<Relationship> getRelationship() {
        return this.relationships;
    }

    public void addRelationship(Relationship relationship) {
        this.relationships.add(relationship);
    }

    @Override
    public String toString() {
        return "OcelEvent [name=" + this.ocelEventType + ", attributes=" + attributes + "]";
    }
}