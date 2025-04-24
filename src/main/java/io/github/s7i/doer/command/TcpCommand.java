package io.github.s7i.doer.command;

import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.DoerException;
import io.github.s7i.doer.domain.kafka.KafkaConfig;
import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.github.s7i.meshtastic.TcpInterface;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@CommandLine.Command(name = "tcp")
@Slf4j
public class TcpCommand extends Command {

    @Slf4j
    @Accessors(fluent = true)
    @Getter
    public static class Options implements KafkaConfig {

        @Option(names = "--nodes", description = "Send at begin request for config with Nodes.")
        boolean configWithNodes;
        @Option(names = "-q", description = "Quiet, less verbose.")
        boolean quiet;

        @Option(names = "--kafka-config", description = "Kafka properties file.")
        String kafkaConfig;
        @Option(names = "--kafka-topic", defaultValue = "meshtastic-from-radio")
        String kafkaTopic;
        @Option(names = "--commit-timeout", description = "Kafka Commit Timeout second.", defaultValue = "30")
        int kafkaAsyncCommitTimeout;
        @Option(names = "--pool-duration", defaultValue = "1")
        int poolDuration;

        @Option(names = "--kafka-rx-topic", defaultValue = "meshtastic-to-radio")
        String kafkaToRadioTopic;

        @Override
        public String getKafkaPropFile() {
            log.debug("using config file: {}", kafkaConfig);
            return requireNonNull(kafkaConfig);
        }

        @Override
        public Map<String, String> getKafka() {
            return Map.of();
        }
    }

    @Mixin
    private Options options;


    @Parameters(arity = "1..2")
    String[] args;


    private class KafkaSender {

        private final Producer<String, byte[]> producer = initKafkaProducer();
        private Thread cthx;

        ProducerRecord<String, byte[]> record(byte[] data) {
            var topic = options.kafkaTopic();
            var key = args[0];

            return new ProducerRecord<>(topic, key, data);
        }

        Optional<RecordMetadata> send(byte[] data) throws Exception {
            if (producer == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(producer.send(record(data))
                  .get(options.kafkaAsyncCommitTimeout(), TimeUnit.SECONDS));
        }

        private Producer<String, byte[]> initKafkaProducer() {
            if (options.kafkaConfig == null || options.kafkaTopic == null) {
                return null;
            }
            return new KafkaFactory()
                  .getProducerFactory()
                  .createProducer(options, false);
        }

        public void bind(Consumer<byte[]> sender) {
            if (options.kafkaToRadioTopic != null && options.kafkaConfig != null) {
                var consumer = new KafkaFactory().getConsumerFactory().createConsumer(options, false);
                cthx = new Thread(() -> {
                    consumer.subscribe(List.of(options.kafkaToRadioTopic));
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            var result = consumer.poll(Duration.of(options.poolDuration, ChronoUnit.SECONDS));
                            result.forEach(r -> {
                                if (r.key() == null) {
                                    sender.accept(r.value());
                                } else if (args[0].equals(r.key())) {
                                    sender.accept(r.value());
                                }
                            });
                            consumer.close();
                        }
                    } catch (Exception e) {
                        log.error("kafka to radio", e);
                    } finally {
                        consumer.close();
                    }

                }, "ToRadio Consumer");
                cthx.setDaemon(true);
                cthx.start();
            }
        }

        private void close() throws InterruptedException {
            cthx.interrupt();
            TimeUnit.SECONDS.sleep(options.poolDuration);

        }
    }

    @Override
    public void onExecuteCommand() {
        try {
            var sender = new KafkaSender();
            String host = args[0];

            int port = Integer.parseInt(args[1]);

            var endTrigger = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(endTrigger::countDown));

            var meshtastic = new TcpInterface(port, host);
            meshtastic.handleFromRadio(data -> {
                try {
                    sender.send(data);
                } catch (Exception e) {
                    log.error("while send", e);
                }
            });
            meshtastic.connect();
            sender.bind(meshtastic::sendToRadio);

            endTrigger.await();
            sender.close();
            meshtastic.disconnect();

        } catch (Exception e) {
            throw new DoerException(e);
        }
    }
}
