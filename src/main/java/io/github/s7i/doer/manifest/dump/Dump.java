package io.github.s7i.doer.manifest.dump;

import io.github.s7i.doer.config.KafkaConfig;
import io.github.s7i.doer.manifest.Manifest;
import io.github.s7i.doer.util.Mutable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class Dump extends KafkaConfig implements Manifest<DumpManifest>, Mutable<Dump> {

    DumpManifest dump;

    @Override
    public DumpManifest getSpecification() {
        return getDump();
    }

    @Override
    public Dump mutate() {
        final var inst = toBuilder().build();
        inst.getParams().putAll(getParams());
        return inst;
    }
}
