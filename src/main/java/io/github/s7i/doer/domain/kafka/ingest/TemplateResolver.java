package io.github.s7i.doer.domain.kafka.ingest;

import com.google.protobuf.Descriptors.Descriptor;
import io.github.s7i.doer.manifest.ingest.Entry;
import io.github.s7i.doer.manifest.ingest.TemplateProp;
import io.github.s7i.doer.manifest.ingest.ValueSet;
import io.github.s7i.doer.proto.Decoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;

@Builder
public class TemplateResolver {

    Entry entry;
    ValueSet valueSet;
    String template;
    Decoder decoder;

    public Optional<TopicEntry> makeTopicEntry(RowProcessor rower) {
        var payload = rower.resolve(template);
        var rowKey = entry.getKey();
        var filledKey = rower.resolve(rowKey);

        try {
            byte[] data;
            if (entry.isProto()) {
                data = asBinaryProto(payload);
            } else {
                data = payload.getBytes(StandardCharsets.UTF_8);
            }
            var topicEntry = new TopicEntry(filledKey, data);
            Header.assignHeaders(entry, topicEntry);

            return Optional.of(topicEntry);
        } catch (RuntimeException e) {
            //do nothing
            return Optional.empty();
        }
    }

    private byte[] asBinaryProto(String payload) {
        return decoder.toMessage(findDescriptor(), payload).toByteArray();
    }

    private Descriptor findDescriptor() {
        return decoder.findMessageDescriptor(entry.getValueTemplate().getProtoMessage());
    }

    public Stream<TopicEntry> topicEntries() {
        List<String> attributes;
        Stream<List<String>> stream;
        if (ValueSet.EMPTY == valueSet) {
            attributes = entry.getValueTemplate()
                  .getProperties()
                  .stream()
                  .map(TemplateProp::getName)
                  .collect(Collectors.toList());

            var list = entry.getValueTemplate()
                  .getProperties()
                  .stream()
                  .map(TemplateProp::getValue)
                  .collect(Collectors.toList());

            stream = Stream.of(list);
        } else {
            attributes = valueSet.getAttributes();
            stream = valueSet.stream();
        }
        var rp = new RowProcessor(attributes);
        return stream
              .map(rp::nextRowValues)
              .map(r -> r.updateTemplateProperties(entry.getValueTemplate()))
              .map(this::makeTopicEntry)
              .filter(Optional::isPresent)
              .map(Optional::get);
    }
}
