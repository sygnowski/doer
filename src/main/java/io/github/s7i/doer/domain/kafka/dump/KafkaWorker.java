package io.github.s7i.doer.domain.kafka.dump;

import static io.github.s7i.doer.Doer.console;
import static io.github.s7i.doer.util.Utils.hasAnyValue;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.google.protobuf.Descriptors.Descriptor;
import io.github.s7i.doer.Doer;
import io.github.s7i.doer.command.dump.ProtoJsonWriter;
import io.github.s7i.doer.command.dump.RecordWriter;
import io.github.s7i.doer.config.KafkaConfig;
import io.github.s7i.doer.config.Range;
import io.github.s7i.doer.domain.kafka.Context;
import io.github.s7i.doer.domain.output.ConsoleOutput;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.domain.rule.Rule;
import io.github.s7i.doer.manifest.dump.DumpManifest;
import io.github.s7i.doer.manifest.dump.Topic;
import io.github.s7i.doer.proto.Decoder;
import io.github.s7i.doer.util.TopicNameResolver;
import io.github.s7i.doer.util.TopicWithResolvableName;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.apache.kafka.common.errors.WakeupException;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
class KafkaWorker implements Context {

    public static final String FLAG_RAW_DATA = "raw-data";

    final DumpManifest specification;
    final KafkaConfig kafkaConfig;
    long recordCounter;
    int poolSize;
    Decoder protoDecoder;
    Map<String, TopicContext> contexts = new HashMap<>();
    boolean useRawData;

    ProtoJsonWriter jsonWriter = (topic, data) -> protoDecoder.toJson(contexts.get(topic).getDescriptor(), data, true);

    boolean keepRunning;

    public void pool() {
        final var topics = resolveTopicNames();
        final var timeout = Duration.ofSeconds(specification.getPoolTimeoutSec());
        initialize();

        keepRunning = true;
        try (Consumer<String, byte[]> consumer = getKafkaFactory().getConsumerFactory().createConsumer(kafkaConfig, hasFlag(Doer.FLAG_USE_TRACING))) {
            addStopHook(() -> {
                log.debug("run wakeup");
                keepRunning = false;
                consumer.wakeup();
            });
            consumer.subscribe(topics, new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {

                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    consumer.committed(new HashSet<>(partitions)).forEach((tp, offset) -> console().info("Offset {} : {}", tp, offset));
                    for (var tp : partitions) {
                        var ctx = contexts.get(tp.topic());
                        if (nonNull(ctx) && nonNull(ctx.getRange()) && ctx.getRange().hasFrom()) {
                            var offset = ctx.getRange().getFrom();
                            console().info("seeking to offset {} on partition {}", offset, tp);
                            consumer.seek(tp, offset);
                        }
                    }
                }
            });
            do {

                var records = consumer.poll(timeout);
                poolSize = records.count();
                if (poolSize > 0) {
                    log.debug("Pool size: {}", poolSize);
                    records.forEach(this::dumpRecord);
                }
            } while (notEnds());
        } catch (WakeupException w) {
            log.debug("wakeup", w);
        }
        console().info("Stop dumping from Kafka, saved records: {}", recordCounter);
    }

    private List<String> resolveTopicNames() {
        var tnr = new TopicNameResolver();
        var topics = specification.getTopics()
              .stream()
              .map(tnr::resolve)
              .map(TopicWithResolvableName::getName)
              .collect(Collectors.toList());
        return topics;
    }

    private void initialize() {
        useRawData = hasFlag(FLAG_RAW_DATA);

        var proto = initProto();
        var ranges = initRange();

        for (var topic : specification.getTopics()) {
            var name = topic.getName();
            var context = contexts.computeIfAbsent(name, TopicContext::new);
            context.setRecordWriter(new RecordWriter(topic, jsonWriter));

            if (nonNull(topic.getRule())) {
                context.setRule(new Rule(topic.getRule()));
            }

            var output = createOutput(topic);
            output.open();
            context.setOutput(output);
            context.setRange(ranges.get(name));
            context.setDescriptor(proto.get(name));
        }
    }

    private Output createOutput(Topic topic) {
        return buildOutput(() -> topic.getOutput().orElse(ConsoleOutput.CONSOLE));
    }

    private Map<String, Range> initRange() {
        return specification.getTopics()
              .stream()
              .filter(t -> hasAnyValue(t.getRange()))
              .collect(Collectors.toConcurrentMap(Topic::getName, topic -> {
                  var range = new Range(topic.getRange());
                  log.debug("Topic {} with range: {}", topic.getName(), range);
                  return range;
              }));
    }

    private Map<String, Descriptor> initProto() {
        var protoSpec = specification.getProto();
        if (nonNull(protoSpec)) {
            protoDecoder = new Decoder();
            protoDecoder.loadDescriptors(protoSpec);

            return specification.getTopics()
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
        return keepRunning && (!hasRange || unExhaustedRanges > 0);
    }

    private void dumpRecord(ConsumerRecord<String, byte[]> record) {
        var ctx = contexts.get(record.topic());
        if (isNull(ctx)) {
            log.error("missing context for topic {}, contexts {}", record.topic(), contexts);
        }
        var lastOffset = record.offset();
        ctx.setLastOffset(lastOffset);

        var range = ctx.getRange();
        if (nonNull(range) && range.positionNotInRange(lastOffset)) {
            return;
        }
        if (ctx.hasRule()) {
            var pass = ctx.getRule().testRule(jsonWriter.toJson(record.topic(), record.value()));
            if (!pass) {
                return;
            }
        }

        final byte[] data;
        if (useRawData) {
            data = record.value();
        } else {
            var txt = ctx.getRecordWriter().toJsonString(record);
            data = txt.getBytes(StandardCharsets.UTF_8);
        }

        var resource = record.topic() + "-" + record.partition() + "-" + lastOffset;
        ctx.getOutput().emit(resource, data);

        recordCounter++;
    }
}
