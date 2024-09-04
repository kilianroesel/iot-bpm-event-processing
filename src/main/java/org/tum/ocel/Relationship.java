package org.tum.ocel;

public class Relationship {
    private OcelObject object;
    private String qualifier;

    public Relationship(OcelObject object, String qualifier) {
        this.object = object;
        this.qualifier = qualifier;
    }


    public String getQualifier() {
        return this.qualifier;
    }
    
    public OcelObject getObject() {
        return this.object;
    }
}
