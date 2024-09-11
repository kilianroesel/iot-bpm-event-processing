package org.tum.bpm.jobs;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.tum.bpm.sources.RulesSource;

public class TestPipeline {
    public static void main(String[] args) throws Exception {

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment().setParallelism(1);

        DataStream<String> bootstrapRuleStream = env
                .fromSource(RulesSource.createRulesIncrementalSource(),
                RulesSource.createWatermarkStrategy(), "Rule Update Source");

        

        bootstrapRuleStream.print();
        env.execute("Testing flink consumer");
    }
}
