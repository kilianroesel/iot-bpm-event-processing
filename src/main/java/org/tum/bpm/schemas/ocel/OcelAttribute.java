package org.tum.bpm.schemas.ocel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcelAttribute {
    private String name;
    private Object value;
}
