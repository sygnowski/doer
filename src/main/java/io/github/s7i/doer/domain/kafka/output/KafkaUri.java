package io.github.s7i.doer.domain.kafka.output;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.DoerException;
import io.github.s7i.doer.Globals;
import io.github.s7i.doer.domain.kafka.KafkaConfig;
import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.domain.output.OutputProvider;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.TopicPartition;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Properties;

import static java.util.Objects.nonNull;

@RequiredArgsConstructor
public class KafkaUri implements KafkaOutputCreator {

    private TopicPartition topic;
    private KafkaConfig config;

    protected final OutputProvider provider;
    protected final Context context;
    private boolean useTrace;

    @Override
    public Output create() {
        useTrace = context.hasFlag("trace");
        try {
            var uri = new URI(provider.getOutput());
            var configPropertyName = uri.getAuthority();
            var kafkaConfig = context.getParams().get(configPropertyName);
            if (nonNull(kafkaConfig)) {
                config = () -> {
                    try {
                        var result = new HashMap<String, String>();

                        final var props = new Properties();
                        props.load(new StringReader(kafkaConfig));
                        props.forEach((k, v) -> result.put((String) k, (String) v));
                        return result;
                    } catch (IOException e) {
                        throw new DoerException(e);
                    }
                };
            } else {
                throw new IllegalStateException("missing kafka property with configuration: " + configPropertyName);
            }
            var topicName = uri.getPath().substring(1);

            topic = new TopicPartition(topicName, 0);
        } catch (URISyntaxException e) {
            throw new DoerException(e);
        }

        return KafkaOutputCreator.super.create();
    }

    private KafkaConfig getConfig() {
        return config;
    }


    @Override
    public KafkaConfig getKafkaConfig() {
        return getConfig();
    }

    @Override
    public boolean getUseTracing() {
        return useTrace;
    }

    @Override
    public KafkaFactory getKafkaFactory() {
        return Globals.INSTANCE.getKafka();
    }

    @Override
    public TopicPartition getTopic() {
        return topic;
    }
}
