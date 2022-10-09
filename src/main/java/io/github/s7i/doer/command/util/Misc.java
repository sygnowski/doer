package io.github.s7i.doer.command.util;

import io.github.s7i.doer.Doer;
import io.github.s7i.doer.command.file.ReplaceInFile;
import io.github.s7i.doer.util.GitProps;
import io.github.s7i.doer.util.PropertyResolver;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import static java.util.Objects.nonNull;

@Command(
        name = "misc",
        description = "Miscellaneous command set.",
        subcommands = {
                ReplaceInFile.class,
                CommandManifest.class
        }
)
@Slf4j(topic = "doer.console")
public class Misc {


    @Command
    public void time(@Parameters(paramLabel = "time-value") long time,
                     @Option(names = "--zone", defaultValue = "Z") String zone,
                     @Option(names = {"--millis"}) boolean millis) {
        ZoneOffset zoneOffset = ZoneOffset.of(zone);

        var instant = millis
                ? Instant.ofEpochMilli(time)
                : Instant.ofEpochSecond(time);

        var date = ZonedDateTime.ofInstant(instant, zoneOffset);
        log.info("epoch: {} > {} in {}", time, date, zoneOffset);
    }

    @Command(name = "text")
    public void text(
            @Parameters(paramLabel = "text", arity = "1", description = "input text") String input,
            @Option(names = {"-p"}) Map<String, String> param) {

        log.info(new PropertyResolver(param).resolve(input));

    }

    @Command
    public void info() throws IOException {
        log.info("DOER_HOME = {}", System.getenv().get("DOER_HOME"));
        log.info("DOER_CONFIG = {}", System.getenv().get(Doer.ENV_CONFIG));

        try (var br = new BufferedReader(new InputStreamReader(Misc.class.getResourceAsStream(GitProps.GIT_PROPERTIES)))) {
            br.lines().forEach(log::info);
        }
    }

    @SneakyThrows
    @Command(name = "clean-log")
    public void cleanLogs() {
        var home = System.getenv("DOER_HOME");

        if (nonNull(home)) {
            var path = Path.of(home, "logs");
            if (Files.isDirectory(path)) {
                Files.list(path)
                        .filter(Files::isRegularFile)
                        .peek(p -> log.info("rm {}", p))
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                                log.warn("oops", e);
                            }
                        });
            }
        }
    }
}
