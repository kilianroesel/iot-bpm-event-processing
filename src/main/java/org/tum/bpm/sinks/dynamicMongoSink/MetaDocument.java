package org.tum.bpm.sinks.dynamicMongoSink;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MetaDocument<T> {
    private String device;
    private T document;
}
