package io.github.s7i.doer.manifest.ingest;

import io.github.s7i.doer.config.Base;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Ingest extends Base {

    Map<String, String> kafka;
    IngestManifest ingest;

}
