package org.tum.bpm.sinks.dynamicMongoSink;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MetaDocument<T> {

    public MetaDocument(String device, Instant eventTime, Instant messageIngestionTime, T document) {
        this.sinkTime = Instant.now(); // Capture serialization time
        this.device = device;
        this.eventTime = eventTime;
        this.ingestionTime = messageIngestionTime;
        this.document = document;
    }

    private String device;
    private Instant eventTime;
    private Instant ingestionTime;
    private Instant sinkTime;
    private T document;
}
