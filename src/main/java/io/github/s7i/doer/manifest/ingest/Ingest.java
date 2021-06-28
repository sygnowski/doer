package io.github.s7i.doer.manifest.ingest;

import io.github.s7i.doer.config.KafkaConfig;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Ingest extends KafkaConfig {

    IngestManifest ingest;
}
