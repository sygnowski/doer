package io.github.s7i.doer.domain.kafka.dump;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

@Slf4j
public class CommitDataAggregator {

    private final Map<TopicPartition, Long> offsets = new HashMap<>();

    public void add(String topic, int partition, long offset) {
        offsets.put(new TopicPartition(topic, partition), offset);
    }

    public Map<TopicPartition, OffsetAndMetadata> toCommit() {

        if (offsets.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<TopicPartition, OffsetAndMetadata> commitMap = offsets.entrySet().stream()
              .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> new OffsetAndMetadata(entry.getValue())
              ));

        if (log.isDebugEnabled()) {
            var sb = new StringBuilder();
            for (var e : commitMap.entrySet()) {
                sb.append(e.getKey().topic());
                sb.append("|");
                sb.append(e.getKey().partition());
                sb.append("|");
                sb.append(e.getValue().offset());
            }
            log.debug("Commit Map \n {}", sb);
        }

        offsets.clear();
        return commitMap;
    }

}