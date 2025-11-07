package io.github.s7i.doer.domain.kafka.dump;

import io.github.s7i.doer.ConsoleLog;
import io.github.s7i.doer.DoerException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;

@Slf4j
@RequiredArgsConstructor
public class OffsetCommitter implements ConsoleLog {

    private final OffsetCommitSettings settings;
    private final CommitDataAggregator aggregator = new CommitDataAggregator();

    public void add(ConsumerRecord<?, ?> record) {
        aggregator.add(record.topic(), record.partition(), record.offset());
    }

    public boolean commit(Consumer<?, ?> consumer) {
        final var toCommit = aggregator.toCommit();
        switch (settings.getKind()) {
            case ASYNC:
                log.debug("async commit of {}", toCommit);
                consumer.commitAsync(toCommit, this::offsetCommitCallback);
                break;
            case SYNC:
                var duration = settings.getSyncCommitDeadline();
                try {
                    consumer.commitSync(toCommit, duration);
                } catch (KafkaException k) {
                    log.warn("Kafka : cannot commit", k);
                    return false;
                }
                break;
            default:
                throw new DoerException(new IllegalStateException("illegal state"));
        }
        return true;
    }

    void offsetCommitCallback(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
        log.warn("cannot commit: {}, exception: {}", offsets, exception);
    }
}
