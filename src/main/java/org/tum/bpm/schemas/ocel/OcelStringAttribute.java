package org.tum.bpm.schemas.ocel;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class OcelStringAttribute extends OcelAttribute {
    private String value;
}
