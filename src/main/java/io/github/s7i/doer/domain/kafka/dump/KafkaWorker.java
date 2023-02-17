package io.github.s7i.doer.domain.kafka.dump;

import com.google.protobuf.Descriptors.Descriptor;
import io.github.s7i.doer.Doer;
import io.github.s7i.doer.command.dump.ProtoJsonWriter;
import io.github.s7i.doer.command.dump.RecordWriter;
import io.github.s7i.doer.config.KafkaConfig;
import io.github.s7i.doer.config.Range;
import io.github.s7i.doer.domain.kafka.ConsumerConfigSetup;
import io.github.s7i.doer.domain.kafka.Context;
import io.github.s7i.doer.domain.output.ConsoleOutput;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.domain.rule.Rule;
import io.github.s7i.doer.manifest.dump.DumpManifest;
import io.github.s7i.doer.manifest.dump.Topic;
import io.github.s7i.doer.proto.Decoder;
import io.github.s7i.doer.util.TopicNameResolver;
import io.github.s7i.doer.util.TopicWithResolvableName;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.s7i.doer.Doer.FLAG_RAW_DATA;
import static io.github.s7i.doer.Doer.console;
import static io.github.s7i.doer.util.Utils.hasAnyValue;
import static java.util.Objects.*;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
class KafkaWorker implements Context {

    final DumpManifest specification;
    final KafkaConfig kafkaConfig;
    long recordCounter;
    int poolSize;
    Decoder protoDecoder;
    Map<String, TopicContext> contexts = new HashMap<>();
    boolean useRawData;

    ProtoJsonWriter jsonWriter = (topic, data) -> protoDecoder.toJson(contexts.get(topic).getDescriptor(), data, true);

    boolean keepRunning;
    OffsetCommitter committer;

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
            subscribe(topics, consumer);
            do {

                var records = consumer.poll(timeout);
                poolSize = records.count();
                if (poolSize > 0) {
                    log.debug("Pool size: {}", poolSize);
                    records.forEach(this::dumpRecord);
                    commitOffset(consumer);
                }
            } while (notEnds());
            commitOffset(consumer);
        } catch (WakeupException w) {
            log.debug("wakeup", w);
        }
        console().info("Stop dumping from Kafka, saved records: {}", recordCounter);
    }

    private void subscribe(List<String> topics, Consumer<String, byte[]> consumer) {
        consumer.subscribe(topics, new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                partitions.forEach(p -> log.warn("partition revoked: {}", p));
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                handlePartitionAssigned(consumer, partitions);
            }
        });
    }

    private void handlePartitionAssigned(Consumer<String, byte[]> consumer, Collection<TopicPartition> partitions) {
        consumer.committed(new HashSet<>(partitions)).forEach((tp, offset) -> console().info("Offset {} : {}", tp, offset));

        var jumpToTime = new HashMap<TopicPartition, Long>(partitions.size());
        for (var tp : partitions) {
            var ctx = contexts.get(tp.topic());
            if (nonNull(ctx)) {
                if (nonNull(ctx.getRange()) && ctx.getRange().hasFrom()) {
                    var offset = ctx.getRange().getFrom();
                    console().info("seeking to offset {} on partition {}", offset, tp);
                    consumer.seek(tp, offset);

                } else if (nonNull(ctx.getFromTime())) {
                    jumpToTime.put(tp, ctx.getFromTime().toEpochMilli());
                    console().info("Seeking on {} to time {}", tp, ctx.getFromTime());
                }
            } else {
                log.error("unhandled topic context ({})", tp);
            }
        }
        if (!jumpToTime.isEmpty()) {
            consumer.offsetsForTimes(jumpToTime);
        }
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

            if (hasAnyValue(topic.getRule())) {
                context.setRule(new Rule(topic.getRule()));
            }

            if (hasAnyValue(topic.getFromTime())) {
                context.setFromTime(LocalDateTime.parse(topic.getFromTime()).toInstant(ZoneOffset.UTC));
            }

            var output = createOutput(topic);
            output.open();
            context.setOutput(output);
            context.setRange(ranges.get(name));
            context.setDescriptor(proto.get(name));
        }

        if (!ranges.isEmpty()) {
            var settings = OffsetCommitSettings.DEFAULT;

            console().info("Offset commit control enabled.");

            ConsumerConfigSetup ccs = kafkaConfig::getKafka;
            ccs.disableAutoCommit();
            ccs.configureMaxPool(settings.getMaxPollSize());

            committer = new OffsetCommitter(settings);
        }
    }

    void commitOffset(Consumer<?, ?> consumer) {
        requireNonNull(consumer, "consumer");
        if (nonNull(committer)) {
            if(!committer.commit(consumer)) {
                console().info("Unsuccessful offset commit!");
            }
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

        if (nonNull(committer)) {
            committer.add(record);
        }
    }
}
