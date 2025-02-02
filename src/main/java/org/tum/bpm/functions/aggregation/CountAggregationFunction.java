package org.tum.bpm.functions.aggregation;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.tum.bpm.schemas.measurements.IoTMessageSchema;

public class CountAggregationFunction implements AggregateFunction<IoTMessageSchema, Long, Long> {

  @Override
  public Long createAccumulator() {
    return 0L;
  }

  @Override
  public Long add(IoTMessageSchema value, Long accumulator) {
    return accumulator = accumulator + 1L;
  }

  @Override
  public Long getResult(Long accumulator) {
    return accumulator;
  }

  @Override
  public Long merge(Long a, Long b) {
    return a + b;
  }
}