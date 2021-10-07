package io.github.s7i.doer;

import static io.github.s7i.doer.Doer.console;
import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.domain.output.OutputFactory;
import io.github.s7i.doer.domain.output.OutputKind;
import io.github.s7i.doer.domain.output.OutputProvider;
import io.github.s7i.doer.domain.output.UriResolver;
import io.github.s7i.doer.domain.output.creator.FileOutputCreator;
import io.github.s7i.doer.domain.output.creator.HttpOutputCreator;
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
            console().info("Init shutdown procedure...");
            Globals.INSTANCE.stopHooks.stream().forEach(Runnable::run);
            console().info("Shutdown completed.");
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

    default Output buildOutput(OutputProvider outputProvider) {
        FileOutputCreator foc = () -> getBaseDir().resolve(outputProvider.getOutput());
        HttpOutputCreator http = outputProvider::getOutput;

        getOutputFactory().register(OutputKind.FILE, foc);
        getOutputFactory().register(OutputKind.HTTP, http);

        return getOutputFactory().resolve(new UriResolver(outputProvider.getOutput()))
              .orElseThrow();
    }
}
