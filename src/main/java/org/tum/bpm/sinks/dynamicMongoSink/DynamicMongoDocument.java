package org.tum.bpm.sinks.dynamicMongoSink;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DynamicMongoDocument<T> {
    private String collection;
    private T document;
}
