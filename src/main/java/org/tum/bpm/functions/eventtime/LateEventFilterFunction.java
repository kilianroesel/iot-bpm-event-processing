package org.tum.bpm.functions.eventtime;

import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.tum.bpm.schemas.measurements.CSVMeasurement;

public class LateEventFilterFunction extends ProcessFunction<CSVMeasurement, CSVMeasurement> {

    private transient OutputTag<CSVMeasurement> lateReadingsOutput = new OutputTag<>("late-measurements");

    @Override
    public void processElement(CSVMeasurement value, ProcessFunction<CSVMeasurement, CSVMeasurement>.Context ctx,
            Collector<CSVMeasurement> out) throws Exception {
        if (ctx.timestamp() < ctx.timerService().currentWatermark()) {
            ctx.output(lateReadingsOutput, value);
        } else {
            out.collect(value);
        }
    }
    
}
