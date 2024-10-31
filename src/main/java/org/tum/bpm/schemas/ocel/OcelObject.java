package org.tum.bpm.schemas.ocel;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcelObject {
    private String id;
    private String type;
    private List<OcelAttribute> attributes;
    private List<OcelRelationship> relationships;
}
