package io.github.s7i.doer.manifest.proto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.s7i.doer.config.ProtoDescriptorContainer;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Proto implements ProtoDescriptorContainer {

    @JsonProperty("descriptor_set")
    List<String> descriptorSet;
}
