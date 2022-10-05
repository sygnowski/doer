package io.github.s7i.doer.domain.kafka.ingest;

import io.github.s7i.doer.Globals;
import io.github.s7i.doer.manifest.ingest.Entry;
import io.github.s7i.doer.manifest.ingest.Topic;
import io.github.s7i.doer.util.PropertyResolver;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Delegate;
import org.apache.kafka.clients.producer.ProducerRecord;

@Data
@AllArgsConstructor
public class FeedRecord {

    String topic;
    @Delegate
    TopicEntry entry;

    public static FeedRecord fromSimpleEntry(Entry entry, Topic topic, Function<String, byte[]> binaryEncoder) {

        var resolver = new PropertyResolver(Globals.INSTANCE.getScope().getParams().get());
        var topicName = topic.getName();
        var key = entry.getKey() != null
              ? resolver.resolve(entry.getKey())
              : null;
        var simpleValue = resolver.resolve(entry.getSimpleValue());
        var data = binaryEncoder.apply(simpleValue);
        var topicEntry = new TopicEntry(key, data);
        Header.assignHeaders(entry, topicEntry);
        return new FeedRecord(topicName, topicEntry);
    }

    public ProducerRecord<String, byte[]> toRecord() {
        var record = new ProducerRecord<>(getTopic(), getKey(), getData());
        entry.getHeaders().forEach(h -> record.headers().add(h.getName(), h.getValue()));
        return record;
    }

    public String toSimpleString() {
        return String.format("topic: %s, data: %s", topic, new String(getData()));
    }
}
