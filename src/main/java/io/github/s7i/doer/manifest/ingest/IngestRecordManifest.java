package io.github.s7i.doer.manifest.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.s7i.doer.config.Base;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
public class IngestRecordManifest extends Base {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Record {
        String record;
        String key;
    }

    @JsonProperty("ingest")
    List<Record> records = Collections.emptyList();


}
