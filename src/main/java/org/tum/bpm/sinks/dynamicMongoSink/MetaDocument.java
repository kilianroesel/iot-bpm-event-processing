package org.tum.bpm.sinks.dynamicMongoSink;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MetaDocument<T> {

    private String device;
    private Instant eventTime;
    private Instant sendTime; 
    private Instant ingestionTime;
    private Instant sinkTime;
    private Instant scopeTime;
    private Instant abstractionTime;
    private Instant enrichmentTime;
    private Instant correlationTime;
    private T document;

    public MetaDocument(String device, Instant eventTime, Instant sendTime, Instant ingestionTime, T document, Instant scopeTime, Instant abstractionTime, Instant enrichmentTime, Instant correlationTime) {
        this.sinkTime = Instant.now(); // Capture serialization time
        this.sendTime = sendTime;
        this.device = device;
        this.eventTime = eventTime;
        this.ingestionTime = ingestionTime;
        this.scopeTime = scopeTime;
        this.abstractionTime = abstractionTime;
        this.correlationTime = correlationTime;
        this.enrichmentTime = enrichmentTime;
        this.document = document;
    }
}
