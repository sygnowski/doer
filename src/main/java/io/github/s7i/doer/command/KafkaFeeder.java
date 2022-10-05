package io.github.s7i.doer.command;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import io.github.s7i.doer.ConsoleLog;
import io.github.s7i.doer.Doer;
import io.github.s7i.doer.domain.kafka.Context;
import io.github.s7i.doer.domain.kafka.ingest.FeedRecord;
import io.github.s7i.doer.domain.kafka.ingest.TemplateResolver;
import io.github.s7i.doer.manifest.ingest.Ingest;
import io.github.s7i.doer.manifest.ingest.IngestManifest;
import io.github.s7i.doer.manifest.ingest.Topic;
import io.github.s7i.doer.proto.Decoder;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "kfeed")
@Slf4j
public class KafkaFeeder implements Context, Runnable, YamlParser, ConsoleLog {

    public static final int SEND_TIMEOUT = 10;

    public static KafkaFeeder createCommandInstance(File yaml) {
        var cmd = new KafkaFeeder();
        cmd.yaml = yaml;
        return cmd;
    }

    @Option(names = {"-y", "-yaml"}, defaultValue = "ingest.yml")
    protected File yaml;
    protected Decoder decoder;
    @Option(names = "-t", description = "Use Open Tracing")
    protected boolean useTracing;
    @Option(names = "-l", description = "Allowed Labels")
    protected List<String> allowedLabels;

    private int sentCount, toSend;

    @Override
    public File getYamlFile() {
        if (!yaml.exists()) {
            throw new IllegalStateException("ingestion file doesn't exists: " + yaml);
        }
        return yaml;
    }

    @Override
    public void run() {
        var config = parseYaml(Ingest.class);

        init(config);

        if (hasFlag(Doer.FLAG_DRY_RUN)) {
            produceRecords(config.getIngest()).forEach( r -> info("[Dry-Run] {}",r.toSimpleString()));
        } else {
            publishToKafka(config);
        }
    }

    private void publishToKafka(Ingest config) {
        info("feeding kafka...");

        try (var producer = getKafkaFactory()
              .getProducerFactory()
              .createProducer(config, useTracing || hasFlag(Doer.FLAG_USE_TRACING))) {
            final boolean flagSendAndForget = hasFlag(Doer.FLAG_SEND_AND_FORGET);

            Consumer<ProducerRecord<String, byte[]>> sender = r -> {
                if (flagSendAndForget) {
                    try {
                        var rm = producer.send(r).get(SEND_TIMEOUT, TimeUnit.SECONDS);
                        info("record sent {}", rm);
                        sentCount++;
                    } catch (Exception e) {
                        log.error("async send", e);
                    }
                } else {
                    producer.send(r, (rm, e) -> {
                        if (isNull(e)) {
                            info("record send {}", rm);
                            sentCount++;
                        } else {
                            info("failed to sent: {} on {}", e, rm);
                        }
                    });
                }
            };

            produceRecords(config.getIngest())
                    .map(FeedRecord::toRecord)
                    .peek(r -> toSend++)
                    .forEach(sender);
        }

        info("kafka feeder ends, records sent {} of {}", sentCount, toSend);
    }

    private void init(Ingest config) {
        new Initializer(InitialParameters.builder()
              .workDir(yaml.toPath().toAbsolutePath().getParent())
              .params(config.getParams())
              .build());
    }

    private Stream<FeedRecord> produceRecords(IngestManifest spec) {
        if (nonNull(spec.getProto())) {
            decoder = new Decoder();
            decoder.loadDescriptors(spec.getProto());
        }
        return spec.getTopics().stream()
                .filter(this::isAllowed)
                .flatMap(t -> buildEntries(spec, t));
    }

    private boolean isAllowed(Topic topic) {
        if (nonNull(allowedLabels)) {
            return allowedLabels.contains(topic.getLabel());
        }
        return true;
    }

    protected Stream<FeedRecord> buildEntries(IngestManifest spec, Topic topic) {
        return topic.getEntries()
                .stream()
                .flatMap(entry -> {
            if (entry.isTemplateEntry()) {
                return TemplateResolver.builder()
                      .entry(entry)
                      .valueSet(spec.findValueSet(topic.getValueSet()))
                      .template(spec.findTemplate(entry.getValueTemplate()))
                      .decoder(decoder)
                      .build()
                      .topicEntries()
                      .map(data -> new FeedRecord(topic.getName(), data));
            } else if (entry.isSimpleValue()) {
                var r = FeedRecord.fromSimpleEntry(entry, topic, raw -> entry.lookupForProto()
                      .map(message -> decoder.toBinaryProto(raw, message))
                      .orElseGet(() -> toBinary(raw))
                );
                return Stream.of(r);
            }
            log.warn("invalid entry: {}", entry);
            return Stream.of();
        });
    }

    private byte[] toBinary(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
