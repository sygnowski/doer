package io.github.s7i.doer.manifest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.s7i.doer.config.Base;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Getter
public class SinkManifest extends Base {
    @Data
    public static class SinkSpec {
        String output = "";
        boolean enabled = true;
    }

    @JsonProperty("spec")
    List<SinkSpec> spec;
}
