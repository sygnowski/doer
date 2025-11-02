package io.github.s7i.doer.command.dump;

import io.github.s7i.doer.Doer;
import io.github.s7i.doer.domain.kafka.ConsumerConfigFile;
import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.github.s7i.doer.util.Utils;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaParser;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.InterruptException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "avro-dump")
@Slf4j
public class AvroDump implements Callable<Integer> {

    public static final long BASE_WAIT = 500L;

    record Options(ConsumerConfigFile kafkaConfig, String topic, Integer duration) {

    }

    @RequiredArgsConstructor
    private class SimpleConsumer {

        private final Options options;

        public void init() {
            ConsumerConfigFile config = options.kafkaConfig();
            //config.disableAutoCommit();

            var consumer = new KafkaFactory().getConsumerFactory().createConsumer(config, false);
            var cthx = new Thread(AvroDump.this.workingGroup, () -> {
                consumer.subscribe(List.of(options.topic()));
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        var result = consumer.poll(Duration.of(options.duration(), ChronoUnit.SECONDS));
                        var pooled = StreamSupport.stream(result.spliterator(), false).toList();

                        boolean resultAddAll;
                        int count = 0;
                        do {
                            try {
                                resultAddAll = AvroDump.this.buffer.addAll(pooled);
                            } catch (IllegalStateException e) {
                                resultAddAll = false;
                            }

                            if (!resultAddAll) {
                                count = Math.min(count + 1, 10);
                                if (count > 2) {
                                    log.warn("buffer not changed, current size: {}", AvroDump.this.buffer.size());
                                }
                                TimeUnit.MILLISECONDS.sleep(count * BASE_WAIT);
                            }
                        } while (!resultAddAll);
                    }
                } catch (InterruptException e) {
                    log.info("kafka interrupted...");
                } catch (Exception e) {
                    log.error("Avro Dump General Error", e);
                } finally {
                    try {
                        consumer.close();
                    } catch (InterruptException e) {
                        // no worry
                    }
                }
                log.info("End of Consuming.");

            }, " Dump Consumer");
            cthx.setDaemon(true);
            cthx.start();
        }
    }


    @Option(names = "--dump", description = "Avro dump file prefix.", defaultValue = "dump")
    String prefix;

    @Option(names = "--kafka", description = "Kafka Property File.", required = true)
    String kafkaConfigPath;

    @Option(names = "--topic", description = "kafka Topic.", required = true)
    String kafkaTopic;

    @Option(names = "--duration", description = "Kafka Pool Duration seconds.", defaultValue = "5000")
    Integer duration;

    @Option(names = "--buffer-size", defaultValue = "700")
    Integer bufferSize;

    private ArrayBlockingQueue<ConsumerRecord<String, byte[]>> buffer;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final ThreadGroup workingGroup = new ThreadGroup("Dump Working Group");


    private void shutdown() {
        workingGroup.interrupt();

        while (workingGroup.activeCount() > 0) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                // no worry
            }
        }

        latch.countDown();
        log.info("[shutdown] completed");
    }


    @Override
    public Integer call() throws Exception {
        buffer = new ArrayBlockingQueue<>(bufferSize);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "shutdown"));

        var dumpFile = new File(prefix + "-" + System.currentTimeMillis() + ".avro");
        log.info("Dump file: {}", dumpFile);
        try {
            var schema = new SchemaParser()
                  .parse(Utils.resource("/kafka_record.avsc"))
                  .mainSchema();
            try (var dataFileWriter = new DataFileWriter<>(new GenericDatumWriter<>(schema))) {
                dataFileWriter.create(schema, dumpFile);
                new Thread(workingGroup, () -> dumpLoop(dataFileWriter, schema), "Dump Worker").start();
                fetchFormKafka();
                latch.await();
            }
        } catch (Exception e) {
            log.error("oops", e);
            return Doer.EC_ERROR;
        }

        return 0;
    }

    private void fetchFormKafka() {
        new SimpleConsumer(
              new Options(
                    new ConsumerConfigFile(kafkaConfigPath),
                    kafkaTopic,
                    duration
              )
        ).init();
    }

    void dumpLoop(DataFileWriter<Object> dataFileWriter, Schema schema) {
        while (!Thread.currentThread().isInterrupted()) {
            do {
                try {
                    var record = buffer.poll(1, TimeUnit.SECONDS);
                    if (record != null) {
                        ByteBuffer key = null;
                        if (record.key() != null) {
                            key = ByteBuffer.wrap(record.key().getBytes(StandardCharsets.UTF_8));
                        }
                        ByteBuffer value = null;
                        if (record.value() != null) {
                            value = ByteBuffer.wrap(record.value());
                        }

                        GenericRecord avroRecord = new GenericData.Record(schema);
                        avroRecord.put("key", key);
                        avroRecord.put("topic", record.topic());
                        avroRecord.put("partition", record.partition());
                        avroRecord.put("offset", record.offset());
                        avroRecord.put("timestamp", record.timestamp());
                        avroRecord.put("value", value);

                        dataFileWriter.append(avroRecord);
                    } else {
                        TimeUnit.MILLISECONDS.sleep(100);
                        dataFileWriter.flush();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("Interrupted, but cleaning buffer, left: {}", buffer.size());
                } catch (AvroRuntimeException | IOException e) {
                    log.warn("oops", e);
                }
            } while (!buffer.isEmpty());
        }
        log.info("End of Avro Writes, buffer size: {}", buffer.size());
    }
}
