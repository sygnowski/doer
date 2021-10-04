package io.github.s7i.doer.domain.kafka.dump;

import static io.github.s7i.doer.util.Utils.hasAnyValue;
import static java.util.Objects.nonNull;

import com.google.protobuf.Descriptors.Descriptor;
import io.github.s7i.doer.command.dump.ProtoJsonWriter;
import io.github.s7i.doer.command.dump.RecordWriter;
import io.github.s7i.doer.config.Dump;
import io.github.s7i.doer.config.Dump.Topic;
import io.github.s7i.doer.config.Range;
import io.github.s7i.doer.domain.kafka.Context;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.domain.output.OutputKind;
import io.github.s7i.doer.domain.output.UriResolver;
import io.github.s7i.doer.domain.output.creator.FileOutputCreator;
import io.github.s7i.doer.proto.Decoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class KafkaWorker implements ProtoJsonWriter, Context {

    final Dump mainConfig;
    long recordCounter;
    int poolSize;
    Decoder protoDecoder;
    Map<String, TopicContext> contexts = new HashMap<>();

    @Override
    public String toJson(String topic, byte[] data) {
        return protoDecoder.toJson(contexts.get(topic).getDescriptor(), data);
    }

    public void pool() {
        var topics = mainConfig.getDump().getTopics()
              .stream()
              .map(t -> t.getName())
              .collect(Collectors.toList());
        final var timeout = Duration.ofSeconds(mainConfig.getDump().getPoolTimeoutSec());
        initialize();

        try (Consumer<String, byte[]> consumer = getKafkaFactory().getConsumerFactory().createConsumer(mainConfig)) {
            consumer.subscribe(topics, new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {

                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    consumer.committed(new HashSet<>(partitions)).forEach((tp, offset) -> log.info("current offset {} for {}", tp, offset));
                    for (var tp : partitions) {
                        var ctx = contexts.get(tp.topic());
                        if (nonNull(ctx) && nonNull(ctx.getRange()) && ctx.getRange().hasFrom()) {
                            var offset = ctx.getRange().getFrom();
                            log.info("seeking to offset {} on partition {}", offset, tp);
                            consumer.seek(tp, offset);
                        }
                    }
                }
            });
            do {

                var records = consumer.poll(timeout);
                poolSize = records.count();
                log.debug("Kafka pool size: {}", poolSize);

                records.forEach(this::dumpRecord);
            } while (notEnds());
        }
        log.info("Stop dumping from Kafka, saved records: {}", recordCounter);
    }

    private void initialize() {
        var proto = initProto();
        var ranges = initRange();

        for (var topic : mainConfig.getDump().getTopics()) {
            var name = topic.getName();
            var context = contexts.computeIfAbsent(name, TopicContext::new);
            context.setRecordWriter(new RecordWriter(topic, this));

            var output = createOutput(topic);
            output.open();
            context.setOutput(output);
            context.setRange(ranges.get(name));
            context.setDescriptor(proto.get(name));
        }
    }

    private Output createOutput(Topic topic) {
        FileOutputCreator foc = () -> getBaseDir().resolve(topic.getOutput());
        getOutputFactory().register(OutputKind.FILE, foc);

        return getOutputFactory().resolve(new UriResolver(topic.getOutput()))
              .orElseThrow();
    }

    private Map<String, Range> initRange() {
        return mainConfig.getDump().getTopics()
              .stream()
              .filter(t -> hasAnyValue(t.getRange()))
              .collect(Collectors.toConcurrentMap(Topic::getName, topic -> {
                  var range = new Range(topic.getRange());
                  log.info("Topic {} with range: {}", topic.getName(), range);
                  return range;
              }));
    }

    private Map<String, Descriptor> initProto() {
        var protoSpec = mainConfig.getDump().getProto();
        if (nonNull(protoSpec)) {
            protoDecoder = new Decoder();
            protoDecoder.loadDescriptors(protoSpec);

            return mainConfig.getDump()
                  .getTopics()
                  .stream()
                  .filter(t -> hasAnyValue(t.getValue().getProtoMessage()))
                  .collect(Collectors.toMap(
                        Topic::getName,
                        t -> protoDecoder.findMessageDescriptor(t.getValue().getProtoMessage())
                  ));
        }
        return Collections.emptyMap();
    }

    private boolean notEnds() {
        var hasRange = contexts.values().stream().anyMatch(TopicContext::hasRange);
        var unExhaustedRanges = !hasRange
              ? 0
              : contexts.values()
                    .stream()
                    .filter(TopicContext::hasRecordsToCollect)
                    .count();
        return !hasRange || unExhaustedRanges > 0;
    }

    private void dumpRecord(ConsumerRecord<String, byte[]> record) {
        var ctx = contexts.get(record.topic());
        var lastOffset = record.offset();
        ctx.setLastOffset(lastOffset);

        var range = ctx.getRange();
        if (nonNull(range) && range.positionNotInRange(lastOffset)) {
            return;
        }
        var txt = ctx.getRecordWriter().toJsonString(record);

        ctx.getOutput().emit(String.valueOf(lastOffset), txt.getBytes(StandardCharsets.UTF_8));

        recordCounter++;
    }
}
