package io.github.s7i.doer.command.dump;

import static io.github.s7i.doer.Utils.hasAnyValue;

import io.github.s7i.doer.config.Dump.Topic;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecordWriter {

    public static final String NEWLINE = "\n";
    final Topic specs;
    final ProtoJsonWriter protoJsonWriter;

    public String toJsonString(ConsumerRecord<String, byte[]> record) {
        var text = new StringBuilder();
        text.append("KEY: ").append(record.key()).append(NEWLINE);
        text.append("TIMESTAMP: ")
              .append(Instant.ofEpochMilli(record.timestamp()).toString())
              .append(NEWLINE);

        text.append("HEADERS:").append(NEWLINE);
        for (var header : record.headers()) {
            text.append(header.key())
                  .append(": ")
                  .append(new String(header.value()))
                  .append(NEWLINE);
        }
        text.append(NEWLINE);

        var value = record.value();

        if (specs.isShowBinary()) {
            text.append("BINARY_BEGIN").append(NEWLINE);
            text.append(new String(value));
            text.append(NEWLINE).append("BINARY_END").append(NEWLINE);
        }

        if (hasAnyValue(specs.getValue().getProtoMessage())) {
            text.append("PROTO_AS_JSON:").append(NEWLINE);
            var json = protoJsonWriter.toJson(record.topic(), value);
            text.append(json);
        }
        return text.toString();
    }
}
