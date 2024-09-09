package org.tum.bpm.jobs;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.tum.bpm.functions.serialization.BootstrapRuleAlignmentFunction;
import org.tum.bpm.sources.RulesSource;

public class TestPipeline {
    public static void main(String[] args) throws Exception {

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3000);

        DataStream<String> bootstrapRuleStream = env
                .fromSource(RulesSource.createRulesBootstrapSource(),
                        RulesSource.createWatermarkStrategy(), "Rule Bootstrap Source")
                .setParallelism(1).process(new BootstrapRuleAlignmentFunction());

        DataStream<String> changeRuleStream = env
                .fromSource(RulesSource.createRulesUpdateSource(),
                        RulesSource.createWatermarkStrategy(), "Rule Update Source")
                .setParallelism(1);

        DataStream<String> ruleStream = bootstrapRuleStream.union(changeRuleStream);

        changeRuleStream.print();
        ruleStream.print();
        env.execute("Testing flink consumer");
    }
}
