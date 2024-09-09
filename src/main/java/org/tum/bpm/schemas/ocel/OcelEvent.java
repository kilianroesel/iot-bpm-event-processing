package org.tum.bpm.schemas.ocel;

import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcelEvent {
    private String id;
    private String type;
    private ZonedDateTime time;
    private OcelAttribute[] ocelAttributes;
    private OcelRelationship[] ocelRelationships;
}
