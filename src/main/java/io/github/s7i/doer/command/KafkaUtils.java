package io.github.s7i.doer.command;

import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.domain.kafka.ConsumerConfigFile;
import io.github.s7i.doer.domain.kafka.KafkaConfig;
import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.github.s7i.doer.domain.kafka.KafkaPropertiesReader;
import java.io.File;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.TopicPartition;
import picocli.CommandLine;
import picocli.CommandLine.Option;

@Slf4j
@CommandLine.Command(name = "kafka")
public class KafkaUtils {


    @CommandLine.Command(name = "lag")
    Integer cmdShowLag(
          @Option(names = "--config", required = true)
          File config) {

        showLag(new ConsumerConfigFile(config.getPath()));

        return 0;

    }

    void showLag(KafkaConfig config) {
        try {
            var params = KafkaPropertiesReader.read(config);

            var broker = requireNonNull(params.getProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG));
            var groupId = requireNonNull(params.getProperty(CommonClientConfigs.GROUP_ID_CONFIG));

            var props = new Properties();
            props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, broker);

            try (AdminClient adminClient = AdminClient.create(props)) {
                var offsetsResult = adminClient.listConsumerGroupOffsets(groupId);
                var consumerOffsets = offsetsResult.partitionsToOffsetAndMetadata().get();

                if (consumerOffsets.isEmpty()) {
                    System.out.println("No offsets found for group: " + groupId);
                    return;
                }

                try (var consumer = new KafkaFactory().getConsumerFactory().createConsumer(config, false)) {
                    var endOffsets = consumer.endOffsets(consumerOffsets.keySet());

                    System.out.printf("%-30s %-15s %-15s %-15s%n", "Partition", "LogEndOffset", "CommittedOffset", "Lag");
                    for (TopicPartition tp : consumerOffsets.keySet()) {
                        long logEndOffset = endOffsets.get(tp);
                        long committedOffset = consumerOffsets.get(tp).offset();
                        long lag = logEndOffset - committedOffset;

                        System.out.printf("%-30s %-15d %-15d %-15d%n",
                              tp, logEndOffset, committedOffset, lag);
                    }
                }
            }
        } catch (Exception e) {
            log.error("oops", e);
        }
    }

    @CommandLine.Command(name = "alive", description = "Kafka HealthCheck.")
    Integer cmdIsAlive(
          @Option(names = "--config", required = true)
          File config) {

        return isAlive(new ConsumerConfigFile(config.getPath()));
    }

    int isAlive(KafkaConfig config) {
        var params = KafkaPropertiesReader.read(config);

        var broker = requireNonNull(params.getProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG));

        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, broker);
        props.put(CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG, "3000");

        try (AdminClient adminClient = AdminClient.create(props)) {

            var cluster = adminClient.describeCluster();
            var clusterId = cluster.clusterId().get(); // Throws if unreachable
            int nodeCount = cluster.nodes().get().size();

            System.out.printf("✅ Kafka cluster is alive. ID=%s, Brokers=%d%n", clusterId, nodeCount);

        } catch (ExecutionException | InterruptedException e) {
            System.err.println("❌ Kafka cluster is not reachable: " + e.getMessage());
            return 1;
        }
        return 0;
    }
}
