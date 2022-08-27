package io.github.s7i.doer.manifest.dump;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.s7i.doer.manifest.Specification;
import io.github.s7i.doer.manifest.proto.Proto;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DumpManifest implements Specification {

    @JsonProperty("pool_timeout_sec")
    Integer poolTimeoutSec;
    List<Topic> topics;
    Proto proto;

    @Override
    protected DumpManifest clone() throws CloneNotSupportedException {
        var list = nonNull(topics) ? topics.stream()
              .map(Topic::toBuilder)
              .map(Topic.TopicBuilder::build)
              .collect(Collectors.toList()) : null;
        return new DumpManifest(poolTimeoutSec, list, proto);
    }
}
