package org.tum.ocel;

import java.util.ArrayList;
import java.util.List;

public class Ocel {
    private List<OcelEvent> events;
    private List<OcelObject> objects;

    public Ocel() {
        this.events = new ArrayList<>();
        this.objects = new ArrayList<>();
    }

    public void addEvent(OcelEvent event) {
        events.add(event);
    }

    public void addObject(OcelObject obj) {
        objects.add(obj);
    }

    public List<OcelEvent> getEvents() {
        return events;
    }

    public List<OcelObject> getObjects() {
        return objects;
    }

    @Override
    public String toString() {
        return "EventLog{" +
                "events=" + events +
                ", objects=" + objects +
                '}';
    }
}
