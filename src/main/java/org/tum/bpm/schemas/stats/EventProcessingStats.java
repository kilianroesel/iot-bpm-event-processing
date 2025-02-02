package org.tum.bpm.schemas.stats;

import java.time.Duration;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventProcessingStats {
    
    private String key;
    private Duration aggregationDuration;
    private long count;
}
