package io.github.s7i.doer.domain.rule;

import io.github.s7i.doer.domain.kafka.dump.TopicContext;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface RuleContextDataAdapter {

    TopicContext getContext();

    default boolean testRule(ConsumerRecord<? , byte[]> record) {
        var ctx = getContext();
        var rw = ctx.getRecordWriter();

        String ruleContextData;
        if (rw.getSpecs().hasProto()) {
            ruleContextData = rw.getProtoJsonWriter()
                    .toJson(record.topic(), record.value());
        } else {
            ruleContextData = new String(record.value());
        }
        return ctx.getRule().testRule(ruleContextData);
    }
}
