package io.github.s7i.doer.manifest.dump;

import io.github.s7i.doer.config.KafkaConfig;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Dump extends KafkaConfig {

    DumpManifest dump;

}
