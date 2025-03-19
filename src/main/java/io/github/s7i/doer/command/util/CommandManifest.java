package io.github.s7i.doer.command.util;

import static io.github.s7i.doer.Doer.DOER_CONSOLE;
import static io.github.s7i.doer.command.ManifestFileCommand.Builder.fromManifestFile;
import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.Doer;
import io.github.s7i.doer.Globals;
import io.github.s7i.doer.command.Command;
import io.github.s7i.doer.command.Ingest;
import io.github.s7i.doer.command.KafkaFeeder;
import io.github.s7i.doer.command.SinkCommand;
import io.github.s7i.doer.command.dump.KafkaDump;
import io.github.s7i.doer.domain.ConfigProcessor;
import io.github.s7i.doer.util.Banner;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@CommandLine.Command(
      name = "command-manifest",
      aliases = "cm",
      description = "Parsing command manifest yaml file."
)
@Slf4j(topic = DOER_CONSOLE)
public class CommandManifest implements Callable<Integer>, Banner {

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
        final AtomicReference<Duration> duration = new AtomicReference<>(Duration.ZERO);
        final AtomicBoolean failed = new AtomicBoolean();
        Runnable killer;

        @Override
        public void run() {
            log.info("Running task {}", name);
            killer = () -> Thread.currentThread().interrupt();
            var begin = Instant.now();
            try {
                try {
                    var statusCode = coreTask.call();
                    failed.set(statusCode != 0);
                } catch (Exception e) {
                    log.error("[TASK FAILURE]", e);
                    failed.set(true);
                }
            } finally {
                duration.set(Duration.between(begin, Instant.now()));
            }
        }

        public TaskWrapper kill() {
            killer.run();
            return this;
        }

        public String getSummary() {
            return String.format("%sTask %s ends in %s", failed.get() ? "[FAILED] " : "", name, duration);
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
                case "sink":
                    return Optional.of(new TaskWrapper(kind, fromManifestFile(SinkCommand.class, yaml)));
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
    public Integer call() {
        printBanner();
        requireNonNull(yamls, "manifest file set...");

        var tasks = Stream.of(yamls)
              .map(this::mapToTask)
              .flatMap(Optional::stream)
              .collect(Collectors.toList());

        if (tasks.isEmpty()) {
            log.info("Nothing to run...");
            return Doer.EC_INVALID_USAGE;
        }

        var pool = Executors.newFixedThreadPool(Math.min(tasks.size(), MAX_THR_COUNT), this::spawnNewThread);
        tasks.forEach(pool::execute);

        try {
            pool.shutdown();
            if (!pool.awaitTermination(24, TimeUnit.HOURS)) {
                log.warn("Timeout...");

                pool.shutdownNow();
            }

            log.info("All task finished.");
            log.info("Summary:");

            var sum = tasks.stream()
                  .filter(TaskWrapper.class::isInstance)
                  .map(TaskWrapper.class::cast)
                  .filter(t -> t.duration.get() != Duration.ZERO)
                  .mapToLong(task -> {
                      log.info(task.getSummary());
                      return task.duration.get().toNanos();
                  })
                  .sum();

            log.info("Total time: {}.", Duration.ofNanos(sum));

            Globals.INSTANCE.stopAllSilent();

            var hasFailed = tasks.stream()
                  .filter(TaskWrapper.class::isInstance)
                  .anyMatch(t -> ((TaskWrapper) t).failed.get());

            if (hasFailed) {
                return Doer.EC_ERROR;
            }

        } catch (InterruptedException e) {
            log.error("oops", e);
            Thread.currentThread().interrupt();
        }
        return 0;
    }
}
