package org.tum.bpm.functions.util;

import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

public class KeyedEventWatermarkExtraction<T> extends KeyedProcessFunction<String, T, String> {

    @Override
    public void processElement(T value, KeyedProcessFunction<String, T, String>.Context ctx, Collector<String> out)
            throws Exception {
        long currentWatermark = ctx.timerService().currentWatermark();
        long currentTimestamp = ctx.timestamp();

        // Emit currentWatermark
        out.collect("Extracted Watermark: " + currentWatermark + " CurrentTimestamp: " + currentTimestamp + " Key: " + ctx.getCurrentKey());
    }
}
