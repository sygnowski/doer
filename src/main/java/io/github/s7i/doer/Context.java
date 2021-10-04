package io.github.s7i.doer;

import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.domain.output.OutputFactory;
import java.nio.file.Path;

public interface Context {

    class Initializer {

        public Initializer(Path workDir) {
            Globals.INSTANCE.root = () -> workDir;
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "shutdown"));
        }

        private void shutdown() {
            Doer.CONSOLE.info("Init shutdown procedure...");
            Globals.INSTANCE.stopHooks.stream().forEach(Runnable::run);
            Doer.CONSOLE.info("Shutdown completed.");
        }
    }

    default void addStopHook(Runnable runnable) {
        Globals.INSTANCE.stopHooks.add(runnable);
    }

    default OutputFactory getOutputFactory() {
        return Globals.INSTANCE.outputFactory;
    }

    default Path getBaseDir() {
        requireNonNull(Globals.INSTANCE.root);
        return Globals.INSTANCE.root.get();
    }
}
