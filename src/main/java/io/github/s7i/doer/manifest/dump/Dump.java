package io.github.s7i.doer.manifest.dump;

import io.github.s7i.doer.manifest.AbstractManifest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class Dump extends AbstractManifest<DumpManifest> {

    DumpManifest dump;

    @Override
    public DumpManifest getSpecification() {
        return getDump();
    }

    @Override
    public Dump clone() throws CloneNotSupportedException {
        final var inst = new Dump(dump.clone());
        copyDependencies(inst);
        return inst;
    }
}
