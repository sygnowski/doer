package io.github.s7i.doer;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.s7i.doer.Ingest.IngestLine;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "kfeed")
@Slf4j
public class KafkaFeeder implements Runnable {

    @Option(names = {"-y", "-yaml"})
    private File yaml;
    private KafkaProducer<String, byte[]> producer;


    @Override
    public void run() {
        var ingest = parseYaml();
        createProducer(ingest);
        try {
            ingest.getIngest().forEach(this::processIngest);
        } finally {
            producer.close();
        }

        System.out.println("kafka feeder ends");
    }

    private Ingest parseYaml() {
        var objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            return objectMapper.readValue(yaml, Ingest.class);
        } catch (IOException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }

    private void processIngest(IngestLine ingestLine) {

        byte[] message;
        var proto = ingestLine.getValue().getProto();
        if (nonNull(proto)) {

            var path = Paths.get(proto.getJson());
            var paths = Stream.of(proto.getDescriptorSet()).map(Paths::get).collect(Collectors.toList());
            var jsonText = asText(path);

            message = new ProtoProcessor().toMessage(paths, proto.getMessageName(), jsonText).toByteArray();
        } else {
            throw new UnsupportedOperationException();
        }

        send(ingestLine.getTopic(), ingestLine.getKey(), message);
    }

    private String asText(Path path) {
        try {
            var relative = yaml.toPath().getParent();
            relative = relative.resolve(path);
            return Files.readString(relative);
        } catch (IOException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }

    void createProducer(Ingest ingest) {
        var props = new Properties();
        props.putAll(ingest.getKafka());
        producer = new KafkaProducer<>(props);
    }

    void send(String topic, String key, byte[] value) {
        producer.send(new ProducerRecord<>(topic, key, value));
    }
}
