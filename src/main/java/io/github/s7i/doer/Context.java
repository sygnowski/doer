package io.github.s7i.doer;

import io.github.s7i.doer.domain.kafka.output.KafkaOutputCreator;
import io.github.s7i.doer.domain.kafka.output.KafkaUri;
import io.github.s7i.doer.domain.output.*;
import io.github.s7i.doer.domain.output.creator.FileOutputCreator;
import io.github.s7i.doer.domain.output.creator.HttpOutputCreator;
import io.github.s7i.doer.domain.output.creator.PipelineOutputCreator;
import io.github.s7i.doer.util.QuitWatcher;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

import static io.github.s7i.doer.Doer.console;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

public interface Context {

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

        public Context context() {
            return Globals.INSTANCE;
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
        FileOutputCreator foc = () -> getBaseDir().resolve(outputProvider.getOutput());
        HttpOutputCreator http = outputProvider::getOutput;
        KafkaOutputCreator kafka = new KafkaUri(outputProvider, this);
        PipelineOutputCreator pipeline = () -> Globals.INSTANCE.pipeline.connect(outputProvider.getOutput());

        final var factory = getOutputFactory();
        factory.register(OutputKind.FILE, foc);
        factory.register(OutputKind.HTTP, http);
        factory.register(OutputKind.KAFKA, kafka);
        factory.register(OutputKind.PIPELINE, pipeline);

        return factory.resolve(new UriResolver(outputProvider.getOutput()))
                .orElseThrow();
    }

    default Map<String, String> getParams() {
        return Globals.INSTANCE.getScope().getParams().get();
    }

    default boolean hasFlag(String flag) {
        final var flags = getParams().get(Doer.FLAGS);

        if (nonNull(flags)) {
            if (flags.equals(flag)) {
                LoggerFactory.getLogger(Context.class).debug("ON FLAG: {}", flag);
                return true;
            }

            final var split = flags.split("\\,");
            final var hasFlag = Arrays.stream(split)
                    .filter(Predicate.not(String::isBlank))
                    .anyMatch(flag::equals);

            if (hasFlag) {
                LoggerFactory.getLogger(Context.class).debug("ON FLAG: {}", flag);
            }
            return hasFlag;
        }
        return false;
    }
}
