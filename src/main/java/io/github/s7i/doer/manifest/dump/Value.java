package io.github.s7i.doer.manifest.dump;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class Value {

    @JsonProperty("proto_message")
    String protoMessage;
}
