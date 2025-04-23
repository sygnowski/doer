package io.github.s7i.doer.command;

import static java.util.Objects.requireNonNull;

import com.google.protobuf.UnknownFieldSet;
import io.github.s7i.doer.DoerException;
import io.github.s7i.doer.domain.kafka.KafkaConfig;
import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.github.s7i.meshtastic.MeshtasticStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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


    @Parameters(arity = "1..*")
    String[] args;


    private class KafkaSender {

        private final Producer<String, byte[]> producer = initKafkaProducer();

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
    }

    @Override
    public void onExecuteCommand() {
        try {
            var sender = new KafkaSender();
            String host = args[0];

            InetAddress inetAddress = InetAddress.getByName(host);

            int port = Integer.parseInt(args[1]);

            var endTrigger = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(endTrigger::countDown));

            try (var socket = new Socket(inetAddress, port)) {
                socket.setTcpNoDelay(true);
                socket.setSoTimeout(500);

                log.debug("is connected {}", socket.isConnected());

                try (var is = socket.getInputStream()) {
                    try (var os = socket.getOutputStream()) {
                        var ms = new MeshtasticStream(is, os);
                        ms.startReadFromRadio(options.configWithNodes);

                        new Thread(() -> {

                            while (ms.isRunning() && !Thread.currentThread().isInterrupted()) {
                                try {
                                    var data = ms.getPool().poll(100, TimeUnit.MILLISECONDS);
                                    if (data != null) {

                                        var proto = UnknownFieldSet.parseFrom(data).toString();
                                        log.info(proto);

                                        sender.send(data).ifPresent(rmt -> {
                                            log.info("record sent: {}", rmt);
                                        });

                                    }
                                } catch (Exception e) {
                                }
                            }
                            endTrigger.countDown();
                        }
                              , "Package Fetcher").start();

                        endTrigger.await();
                    }
                }
            }
        } catch (Exception e) {
            throw new DoerException(e);
        }
    }
}
