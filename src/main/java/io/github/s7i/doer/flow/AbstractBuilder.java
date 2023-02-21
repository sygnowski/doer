package io.github.s7i.doer.flow;

import io.github.s7i.doer.manifest.Manifest;
import io.github.s7i.doer.manifest.Specification;
import io.github.s7i.doer.util.Mutable;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Accessors(chain = true)
@Slf4j
public abstract class AbstractBuilder {

    @SuppressWarnings("unchecked")
    public <T extends Specification> Manifest<T> makeCopy(Manifest<T> src) {
        if (src instanceof Mutable) {
            try {
                return (Manifest<T>) ((Mutable<T>) src).mutate();
            } catch (Exception e) {
                log.error("", e);
                throw new IllegalStateException(e);
            }
        } else {
            throw new IllegalStateException();
        }
    }

    @Setter
    protected Variant variant;

    public abstract Job build();
}
