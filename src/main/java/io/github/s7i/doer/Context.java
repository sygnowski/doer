package io.github.s7i.doer;

import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.domain.output.OutputFactory;
import java.nio.file.Path;

public interface Context {

    default OutputFactory getOutputFactory() {
        return Globals.INSTANCE.outputFactory;
    }

    default Path getBaseDir() {
        requireNonNull(Globals.INSTANCE.root);
        return Globals.INSTANCE.root.get();
    }
}
