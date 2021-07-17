package io.github.s7i.doer;

import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.github.s7i.doer.domain.output.OutputFactory;
import java.nio.file.Path;
import java.util.function.Supplier;

public enum Globals {
    INSTANCE;
    public Supplier<Path> root;
    public KafkaFactory kafka = new KafkaFactory();
    public OutputFactory outputFactory = new OutputFactory();

}
