package org.tum.bpm.jobs;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.tum.bpm.functions.deserialization.MongoChangeStreamDeserialization;
import org.tum.bpm.functions.deserialization.MongoDbChangeRuleDeserialization;
import org.tum.bpm.schemas.debezium.MongoChangeStreamMessage;
import org.tum.bpm.schemas.rules.Rule;
import org.tum.bpm.schemas.rules.RuleControl;
import org.tum.bpm.sources.RulesSource;

public class TestPipeline {
    public static void main(String[] args) throws Exception {

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment().setParallelism(1);

        DataStream<String> bootstrapRuleStream = env
                .fromSource(RulesSource.createRulesIncrementalSource(),
                RulesSource.createWatermarkStrategy(), "Rule Update Source");
        

        DataStream<MongoChangeStreamMessage> stream = bootstrapRuleStream.process(new MongoChangeStreamDeserialization());
        DataStream<RuleControl<Rule>> ruleControlStream = stream.process(new MongoDbChangeRuleDeserialization());

        ruleControlStream.print();
        env.execute("Testing flink consumer");
    }
}
