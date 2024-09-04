package org.tum.bpm.functions.util;

import org.apache.flink.streaming.api.functions.ProcessFunction;

public class EventWatermarkExtraction<T> extends ProcessFunction<T, String> {

    @Override
    public void processElement(T value, ProcessFunction<T, String>.Context ctx,
            org.apache.flink.util.Collector<String> out) throws Exception {
           long currentWatermark = ctx.timerService().currentWatermark();
    long currentTimestamp= ctx.timestamp();

    // Emit currentWatermark
    out.collect("Extracted Watermark: " + currentWatermark + " CurrentTimestamp: " + currentTimestamp);
    }
}
