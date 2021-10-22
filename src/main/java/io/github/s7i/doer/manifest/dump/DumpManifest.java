package io.github.s7i.doer.manifest.dump;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.s7i.doer.manifest.Specification;
import io.github.s7i.doer.manifest.proto.Proto;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class DumpManifest implements Specification {

    @JsonProperty("pool_timeout_sec")
    Integer poolTimeoutSec;
    List<Topic> topics;
    Proto proto;
}
