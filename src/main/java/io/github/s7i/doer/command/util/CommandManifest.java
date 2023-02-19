package io.github.s7i.doer.command.util;

import io.github.s7i.doer.command.Command;
import io.github.s7i.doer.command.Ingest;
import io.github.s7i.doer.command.KafkaFeeder;
import io.github.s7i.doer.command.dump.KafkaDump;
import io.github.s7i.doer.domain.ConfigProcessor;
import io.github.s7i.doer.util.Banner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.s7i.doer.command.ManifestFileCommand.Builder.fromManifestFile;
import static java.util.Objects.requireNonNull;

@CommandLine.Command(
        name = "command-manifest",
        aliases = "cm",
        description = "Parsing command manifest yaml file."
)
@Slf4j
public class CommandManifest implements Runnable, Banner {

    public static final int MAX_THR_COUNT = 20;
    @Parameters(arity = "1..*")
    File[] yamls;

    int threadNo;

    @Option(names = "-wp")
    List<String> withParam;

    public Thread spawnNewThread(Runnable runnable) {
        return new Thread(runnable, "Doer-" + ++threadNo);
    }

    @RequiredArgsConstructor
    static class TaskWrapper implements Runnable {

        final String name;
        final Command coreTask;

        @Override
        public void run() {
            log.info("Running task {}", name);
            var begin = Instant.now();
            try {
                try {
                    coreTask.call();
                } catch (Exception e) {
                    log.error("fatal error", e);
                }
            } finally {
                var duration = Duration.between(begin, Instant.now());
                log.info("Task {} ends in {}", name, duration);
            }
        }
    }

    private Optional<Runnable> mapToTask(File yaml) {
        try (var br = Files.newBufferedReader(yaml.toPath())) {
            String version = br.readLine();
            String kind = br.readLine();
            kind = kind.substring(kind.lastIndexOf(':') + 1).trim();

            switch (kind) {
                case "kafka-ingest":
                    return Optional.of(new TaskWrapper(kind, fromManifestFile(KafkaFeeder.class, yaml)));
                case "kafka-dump":
                    return Optional.of(new TaskWrapper(kind, fromManifestFile(KafkaDump.class, yaml)));
                case "ingest":
                    return Optional.of(new TaskWrapper(kind, fromManifestFile(Ingest.class, yaml)));
                case "config":
                    return Optional.of(() -> new ConfigProcessor(yaml).processConfig());
                default:
                    log.warn("unsupported kind/type of file: {}", kind);
                    break;
            }

        } catch (IOException e) {
            log.error("reading yaml file", e);
        }
        return Optional.empty();
    }

    @Override
    public void run() {
        printBanner();
        requireNonNull(yamls, "manifest file set...");

        var tasks = Stream.of(yamls)
                .map(this::mapToTask)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        var pool = Executors.newFixedThreadPool(Math.min(tasks.size(), MAX_THR_COUNT), this::spawnNewThread);
        tasks.forEach(pool::execute);

        try {
            pool.shutdown();
            pool.awaitTermination(24, TimeUnit.HOURS);
            log.info("All task ended");
        } catch (InterruptedException e) {
            log.error("", e);
            Thread.currentThread().interrupt();
        }
    }
}
