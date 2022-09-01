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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
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

    private int sentCount;

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
        publishToKafka(config);
    }

    private void publishToKafka(Ingest config) {
        final var records = produceRecords(config.getIngest())
              .stream()
              .map(FeedRecord::toRecord)
              .collect(Collectors.toList());
        final var toSend = records.size();
        info("feeding kafka, prepared records count: {}", toSend);

        try (var producer = getKafkaFactory()
              .getProducerFactory()
              .createProducer(config, useTracing || hasFlag(Doer.FLAG_USE_TRACING))) {
            final boolean flagSendAndForget = hasFlag(Doer.FLAG_SEND_AND_FORGET);

            for (var r : records) {
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
            }
        }

        info("kafka feeder ends, records sent {} of {}", sentCount, toSend);
    }

    private void init(Ingest config) {
        new Initializer(InitialParameters.builder()
              .workDir(yaml.toPath().toAbsolutePath().getParent())
              .params(config.getParams())
              .build());
    }

    private List<FeedRecord> produceRecords(IngestManifest spec) {
        if (nonNull(spec.getProto())) {
            decoder = new Decoder();
            decoder.loadDescriptors(spec.getProto());
        }
        var result = new ArrayList<FeedRecord>();
        for (var topic : spec.getTopics()) {
            if (isAllowed(topic)) {
                buildEntries(spec, result, topic);
            }
        }
        return result;
    }

    private boolean isAllowed(Topic topic) {
        if (nonNull(allowedLabels)) {
            return allowedLabels.contains(topic.getLabel());
        }
        return true;
    }

    protected void buildEntries(IngestManifest spec, ArrayList<FeedRecord> result, Topic topic) {
        for (var entry : topic.getEntries()) {
            if (entry.isTemplateEntry()) {
                TemplateResolver.builder()
                      .entry(entry)
                      .valueSet(spec.findValueSet(topic.getValueSet()))
                      .template(spec.findTemplate(entry.getValueTemplate()))
                      .decoder(decoder)
                      .build()
                      .topicEntries()
                      .forEach(data -> result.add(new FeedRecord(topic.getName(), data)));
            } else if (entry.isSimpleValue()) {
                var r = FeedRecord.fromSimpleEntry(entry, topic, raw -> entry.lookupForProto()
                      .map(message -> decoder.toBinaryProto(raw, message))
                      .orElseGet(() -> toBinary(raw))
                );
                result.add(r);
            }
        }
    }

    private byte[] toBinary(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
