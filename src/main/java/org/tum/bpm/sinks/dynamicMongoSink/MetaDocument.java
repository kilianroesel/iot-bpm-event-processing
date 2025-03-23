package org.tum.bpm.sinks.dynamicMongoSink;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MetaDocument<T> {

    private String device;
    private Instant eventTime;
    private Instant ingestionTime;
    private Instant sinkTime;
    private Instant eventAbstractionTime;
    private Instant eventEnrichmentTime;
    private Instant eventCorrelationTime;
    private T document;

    public MetaDocument(String device, Instant eventTime, Instant messageIngestionTime, T document, Instant eventAbstractionTime, Instant eventEnrichmentTime, Instant eventCorrelationTime) {
        this.sinkTime = Instant.now(); // Capture serialization time
        this.device = device;
        this.eventTime = eventTime;
        this.ingestionTime = messageIngestionTime;
        this.eventAbstractionTime = eventAbstractionTime;
        this.eventCorrelationTime = eventCorrelationTime;
        this.eventEnrichmentTime = eventEnrichmentTime;
        this.document = document;
    }
}
