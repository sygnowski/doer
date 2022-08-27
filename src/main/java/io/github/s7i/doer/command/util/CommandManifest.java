package io.github.s7i.doer.command.util;

import io.github.s7i.doer.command.Ingest;
import io.github.s7i.doer.command.KafkaFeeder;
import io.github.s7i.doer.command.dump.KafkaDump;
import io.github.s7i.doer.domain.ConfigProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Command(name = "command-manifest", description = "Parsing command manifest yaml file")
@Slf4j
public class CommandManifest implements Runnable {

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
        final Runnable coreTask;

        @Override
        public void run() {
            log.info("Running task {}", name);
            var begin = Instant.now();
            try {
                coreTask.run();
            } finally {
                var duration = Duration.between(begin, Instant.now());
                log.info("Task {} ends in {}", name, duration);
            }
        }
    }

    @Override
    public void run() {
        var pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), this::spawnNewThread);
        for (var yaml : yamls) {
            try (var br = Files.newBufferedReader(yaml.toPath())) {
                String version = br.readLine();
                String kind = br.readLine();
                kind = kind.substring(kind.lastIndexOf(':') + 1).trim().toLowerCase();

                switch (kind) {
                    case "kafka-ingest":
                        pool.execute(new TaskWrapper(kind, KafkaFeeder.createCommandInstance(yaml)));
                        break;
                    case "kafka-dump":
                        pool.execute(new TaskWrapper(kind, KafkaDump.createCommandInstance(yaml)));
                        break;
                    case "ingest":
                        pool.execute(new TaskWrapper(kind, Ingest.createCommandInstance(yaml)));
                        break;
                    case "config":
                        pool.execute(new TaskWrapper(kind, () -> new ConfigProcessor(yaml).processConfig()));
                        break;
                    default:
                        log.warn("unsupported kind/type of file: {}", kind);
                        break;

                }

            } catch (IOException e) {
                log.error("reading yaml file", e);
            }
        }
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
