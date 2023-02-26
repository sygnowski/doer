package io.github.s7i.doer;

import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.domain.output.OutputBuilder;
import io.github.s7i.doer.domain.output.OutputFactory;
import io.github.s7i.doer.domain.output.OutputProvider;
import io.github.s7i.doer.pipeline.Pipeline;
import io.github.s7i.doer.util.ParamFlagExtractor;
import io.github.s7i.doer.util.PropertyResolver;
import io.github.s7i.doer.util.QuitWatcher;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.github.s7i.doer.Doer.console;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

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
            Runtime.getRuntime().addShutdownHook(new Thread(Globals.INSTANCE::stopAll, "shutdown"));
        }

        public Initializer(InitialParameters parameters) {
            Pipeline.initFrom(parameters::getParams);
            var scope = Globals.INSTANCE.getScope();
            scope.setRoot(parameters::getWorkDir);
            scope.setParams(parameters::getParams);

            new QuitWatcher().watchForQuit(() -> System.exit(Doer.EC_QUIT));
        }

        public Context context() {
            return Globals.INSTANCE;
        }
    }

    default void addStopHook(Runnable runnable) {
        Globals.INSTANCE.stopHooks.add(runnable);
    }

    default void stopAll() {
        console().info("Init shutdown procedure...");
        Globals.INSTANCE.stopHooks.forEach(Runnable::run);
        console().info("Shutdown completed.");
    }

    default void stopAllSilent() {
        Globals.INSTANCE.stopHooks.forEach(Runnable::run);
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

    default Optional<Pipeline> lookupPipeline() {
        var pipeline = Globals.INSTANCE.pipeline;
        if (nonNull(pipeline) && pipeline.isEnabled()) {
            return Optional.of(pipeline);
        }
        return Optional.empty();
    }

    default void shareParams(Map<String, String> otherParams) {
        var copy = new HashMap<>(otherParams);
        Globals.INSTANCE.getScope().setParams(() -> copy);
    }
}
