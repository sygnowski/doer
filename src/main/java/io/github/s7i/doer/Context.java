package io.github.s7i.doer;

import static io.github.s7i.doer.Doer.console;
import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.domain.output.*;
import io.github.s7i.doer.util.ParamFlagExtractor;
import io.github.s7i.doer.util.PropertyResolver;
import io.github.s7i.doer.util.QuitWatcher;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;

public interface Context extends ParamFlagExtractor {

    @Builder
    @Getter
    class InitialParameters {

        Path workDir;
        @Default
        Map<String, String> params = Collections.emptyMap();
    }

    class Initializer {

        static {
            Runtime.getRuntime().addShutdownHook(new Thread(Initializer::shutdown, "shutdown"));
        }

        public Initializer(InitialParameters parameters) {
            var scope = Globals.INSTANCE.getScope();
            scope.setRoot(parameters::getWorkDir);
            scope.setParams(parameters::getParams);

            new QuitWatcher().watchForQuit(() -> System.exit(Doer.EC_QUIT));
        }

        private static void shutdown() {
            console().info("Init shutdown procedure...");
            Globals.INSTANCE.stopHooks.forEach(Runnable::run);
            console().info("Shutdown completed.");
        }
    }

    default void addStopHook(Runnable runnable) {
        Globals.INSTANCE.stopHooks.add(runnable);
    }

    default OutputFactory getOutputFactory() {
        return Globals.INSTANCE.getScope().getOutputFactory();
    }

    default Path getBaseDir() {
        Path baseDir = Globals.INSTANCE.getScope().getRoot().get();
        requireNonNull(baseDir);
        return baseDir;
    }

    default Output buildOutput(OutputProvider outputProvider) {
        return new OutputBuilder().context(this).build(outputProvider);
    }

    default Map<String, String> getParams() {
        return Globals.INSTANCE.getScope().getParams().get();
    }

    default PropertyResolver getPropertyResolver() {
        return new PropertyResolver(getParams());
    }
}
