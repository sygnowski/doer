package io.github.s7i.doer.domain.output;

import java.nio.file.Path;
import java.util.function.BiFunction;
import lombok.Getter;

public class OutputFactory {

    public OutputFactory() {
        this(OutputFactory::create);
    }

    public OutputFactory(BiFunction<Path, String, Output> creator) {
        this.creator = creator;
    }

    @Getter
    final BiFunction<Path, String, Output> creator;

    static Output create(Path root, String subResource) {
        return new FileOutput(root, subResource);
    }
}
