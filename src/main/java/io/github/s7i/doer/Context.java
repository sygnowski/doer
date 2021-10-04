package io.github.s7i.doer;

import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.domain.output.OutputFactory;
import java.nio.file.Path;

public interface Context {

    class Initializer {

        public Initializer(Path workDir) {
            Globals.INSTANCE.root = () -> workDir;
            Runnable onShutdown = () -> Globals.INSTANCE.stopHooks.stream().forEach(Runnable::run);
            Runtime.getRuntime().addShutdownHook(new Thread(onShutdown, "shutdown"));
        }
    }

    default OutputFactory getOutputFactory() {
        return Globals.INSTANCE.outputFactory;
    }

    default Path getBaseDir() {
        requireNonNull(Globals.INSTANCE.root);
        return Globals.INSTANCE.root.get();
    }
}
