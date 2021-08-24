package io.github.s7i.doer.manifest.ingest;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
public class ValueTemplate {

    @JsonProperty("name")
    String templateName;
    @JsonProperty("proto_message")
    String protoMessage;
    List<TemplateProp> properties;

    public List<TemplateProp> getProperties() {
        if (isNull(properties)) {
            return Collections.emptyList();
        }
        return properties;
    }
}
