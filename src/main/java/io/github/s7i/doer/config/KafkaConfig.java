package io.github.s7i.doer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Getter;

@Getter
public class KafkaConfig extends Base {

    @JsonProperty("kafka-properties")
    String kafkaPropFile;

    Map<String, String> kafka;

}
