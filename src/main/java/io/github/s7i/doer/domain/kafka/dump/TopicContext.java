package io.github.s7i.doer.domain.kafka.dump;

import static java.util.Objects.nonNull;

import com.google.protobuf.Descriptors.Descriptor;
import io.github.s7i.doer.command.dump.RecordWriter;
import io.github.s7i.doer.config.Range;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.domain.rule.Rule;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.ToString.Include;

@Data
@RequiredArgsConstructor
@ToString
public class TopicContext {

    @Include()
    final String name;
    Long lastOffset = 0L;
    Range range;
    Descriptor descriptor;
    Output output;
    RecordWriter recordWriter;
    private Rule rule;

    public boolean hasRange() {
        return nonNull(range);
    }

    public boolean hasRecordsToCollect() {
        return hasRange() && !range.reachEnd(lastOffset);
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    public boolean hasRule() {
        return nonNull(rule);
    }
}
