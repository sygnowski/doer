package io.github.s7i.doer.manifest.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;

@Getter
public class Topic {

    String name;
    String label;
    @JsonProperty("value_set")
    String valueSet;
    List<Entry> entries;
}
