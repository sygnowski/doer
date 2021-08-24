package io.github.s7i.doer.manifest.ingest;

import static io.github.s7i.doer.util.Utils.hasAnyValue;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Optional;
import lombok.Getter;

@Getter
public class Entry {

    String key;
    List<Header> headers;
    @JsonProperty("value_template")
    ValueTemplate valueTemplate;
    @JsonProperty("value")
    String simpleValue;
    @JsonProperty("proto_message")
    String protoMessage;

    public boolean hasHeaders() {
        return nonNull(headers) && !headers.isEmpty();
    }

    public boolean isProto() {
        return nonNull(valueTemplate) && hasAnyValue(valueTemplate.getProtoMessage());
    }


    public boolean isSimpleValue() {
        return hasAnyValue(simpleValue);
    }

    public boolean isTemplateEntry() {
        return nonNull(valueTemplate);
    }

    public Optional<String> lookupForProto() {
        return Optional.ofNullable(protoMessage);
    }
}
