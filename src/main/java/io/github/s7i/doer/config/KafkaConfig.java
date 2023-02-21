package io.github.s7i.doer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Getter
@ToString(callSuper = true)
public class KafkaConfig extends Base implements io.github.s7i.doer.domain.kafka.KafkaConfig {

    @JsonProperty("kafka-properties")
    protected String kafkaPropFile;

    protected Map<String, String> kafka = new HashMap<>();

}
