package io.github.s7i.doer;

import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.domain.output.OutputFactory;
import java.nio.file.Path;

public interface Context {

    class Initializer {

        static {
            Runtime.getRuntime().addShutdownHook(new Thread(Initializer::shutdown, "shutdown"));
        }

        public Initializer(Path workDir) {
            Globals.INSTANCE.getScope().root = () -> workDir;

        }

        private static void shutdown() {
            Doer.CONSOLE.info("Init shutdown procedure...");
            Globals.INSTANCE.stopHooks.stream().forEach(Runnable::run);
            Doer.CONSOLE.info("Shutdown completed.");
        }
    }

    default void addStopHook(Runnable runnable) {
        Globals.INSTANCE.stopHooks.add(runnable);
    }

    default OutputFactory getOutputFactory() {
        return Globals.INSTANCE.getScope().outputFactory;
    }

    default Path getBaseDir() {
        Path baseDir = Globals.INSTANCE.getScope().root.get();
        requireNonNull(baseDir);
        return baseDir;
    }
}
