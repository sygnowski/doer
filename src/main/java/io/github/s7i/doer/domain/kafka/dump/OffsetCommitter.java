package io.github.s7i.doer.domain.kafka.dump;

import io.github.s7i.doer.ConsoleLog;
import io.github.s7i.doer.DoerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class OffsetCommitter implements ConsoleLog {

    private final OffsetCommitSettings settings;
    private final Map<TopicPartition, OffsetAndMetadata> commitMap = new HashMap<>();

    void add(ConsumerRecord<?, ?> record) {
        var tp = new TopicPartition(record.topic(), record.partition());
        var om = new OffsetAndMetadata(record.offset());
        commitMap.put(tp, om);
    }

    public boolean commit(Consumer<?, ?> consumer) {
        if (commitMap.isEmpty()) {
            return true;
        }
        var toCommit = new HashMap<>(commitMap);
        commitMap.clear();

        switch (settings.getKind()) {
            case ASYNC:
                log.debug("async commit of {}", commitMap);
                consumer.commitAsync(toCommit, this::offsetCommitCallback);
                break;
            case SYNC:
                var duration = settings.getSyncCommitDeadline();
                log.debug("sync commit of {} with timeout: {}", commitMap, duration);
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
