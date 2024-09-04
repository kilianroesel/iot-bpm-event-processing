package org.tum.bpm.functions.ruleRefinement;

import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.util.Collector;
import org.tum.bpm.schemas.debezium.DebeziumMessage;
import org.tum.bpm.schemas.rules.RuleControl;

public class RuleRefinementFunction<T>
        extends RichFlatMapFunction<DebeziumMessage<T>, RuleControl<T>> {

    @Override
    public void flatMap(DebeziumMessage<T> value, Collector<RuleControl<T>> out) throws Exception {

        if (value.getOp().equals("r") || value.getOp().equals("c") || value.getOp().equals("u")) {
            RuleControl<T> ruleControl = new RuleControl<T>(value.getAfter(), RuleControl.Control.ACTIVE);
            out.collect(ruleControl);
        }

        if (value.getOp().equals("d")) {
            RuleControl<T> ruleControl = new RuleControl<T>(value.getAfter(), RuleControl.Control.INACTIVE);
            out.collect(ruleControl);
        }
    }
}
