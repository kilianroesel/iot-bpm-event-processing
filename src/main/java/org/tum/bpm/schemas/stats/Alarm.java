package org.tum.bpm.schemas.stats;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Alarm {

    private String alarm;
    private Instant eventTime;
    private Instant alarmTime;


    public Alarm(String alarm, Instant eventTime) {
        this.alarmTime = Instant.now();
        this.alarm = alarm;
        this.eventTime = eventTime;
    }
}
