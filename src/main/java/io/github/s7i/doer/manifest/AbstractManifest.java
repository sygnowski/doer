package io.github.s7i.doer.manifest;

import io.github.s7i.doer.config.KafkaConfig;
import io.github.s7i.doer.util.Mutable;
import java.util.HashMap;
import lombok.SneakyThrows;

public abstract class AbstractManifest<T extends Specification> extends KafkaConfig implements Manifest<T>, Mutable<AbstractManifest<T>>, Cloneable {

    protected void copyDependencies(AbstractManifest<T> target) {
        target.kind = kind;
        target.kafka = kafka;
        target.kafkaPropFile = kafkaPropFile;
        target.params = new HashMap<>(params);
    }


    @SuppressWarnings("unchecked")
    @SneakyThrows
    @Override
    public AbstractManifest<T> mutate() {
        return (AbstractManifest<T>) clone();
    }
}
