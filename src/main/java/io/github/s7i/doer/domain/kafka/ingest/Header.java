package io.github.s7i.doer.domain.kafka.ingest;

import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.manifest.ingest.Entry;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Header {

    public static void assignHeaders(Entry entry, TopicEntry topicEntry) {
        requireNonNull(entry);
        requireNonNull(topicEntry);
        if (entry.hasHeaders()) {
            var headers = entry.getHeaders().stream()
                  .map(h -> Header.from(h.getName(), h.getValue()))
                  .collect(Collectors.toList());
            topicEntry.setHeaders(headers);
        }
    }

    public static Header from(String name, String value) {
        requireNonNull(name, "name");
        requireNonNull(value, "value");
        return new Header(name, value.getBytes(StandardCharsets.UTF_8));
    }

    String name;
    byte[] value;
}
