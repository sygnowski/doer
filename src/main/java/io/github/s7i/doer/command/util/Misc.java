package io.github.s7i.doer.command.util;

import com.google.gson.Gson;
import com.google.protobuf.TextFormat;
import io.github.s7i.doer.Doer;
import io.github.s7i.doer.HandledRuntimeException;
import io.github.s7i.doer.command.file.ReplaceInFile;
import io.github.s7i.doer.domain.output.HttpOutput;
import io.github.s7i.doer.pipeline.PipelineService;
import io.github.s7i.doer.util.Clipboard;
import io.github.s7i.doer.util.GitProps;
import io.github.s7i.doer.util.PropertyResolver;
import io.github.s7i.doer.util.Utils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.fusesource.jansi.AnsiConsole;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.jeasy.rules.mvel.MVELRuleFactory;
import org.jeasy.rules.support.reader.YamlRuleDefinitionReader;
import org.mvel2.MVEL;
import org.mvel2.compiler.CompiledExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;

@Command(
        name = "misc",
        description = "Miscellaneous command set.",
        subcommands = {
                ReplaceInFile.class,
                CommandManifest.class,
                PipelineService.class
        }
)
@Slf4j(topic = "doer.console")
public class Misc {

    public static final Logger LOG = LoggerFactory.getLogger(Misc.class);

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
            @Option(names = {"-p"}, description = "Param:  key=value") Map<String, String> param,
            @Option(names = {"-c", "--color"}, description = "Enable ANSI colors", defaultValue = "false") Boolean colors) {

        final var result = new PropertyResolver(param).resolve(input);
        if (colors) {
            if (SystemUtils.IS_OS_WINDOWS) {
                AnsiConsole.systemInstall();
            }

            System.out.println(CommandLine.Help.Ansi.AUTO.string(result));

            if (SystemUtils.IS_OS_WINDOWS) {
                AnsiConsole.systemInstall();
            }
        } else {
            log.info(result);
        }
    }

    @SneakyThrows
    @Command
    public void unescape(
            @Parameters(arity = "1") String input,
            @Option(names = {"-t", "--type"}, defaultValue = "java") String type,
            @Option(names = {"-p"}, description = "Unescape Proto Bytes") boolean unEscProto) {

        var in = new PropertyResolver().resolve(input);
        String out;
        switch (type) {
            case "none":
                out = in;
                break;
            case "json":
                out = StringEscapeUtils.unescapeJson(in);
                break;
            case "java":
            default:
                out = StringEscapeUtils.unescapeJava(in);
                break;
        }
        if (unEscProto) {
            var bs = TextFormat.unescapeBytes(out);
            bs.writeTo(System.out);
            return;
        }
        System.out.println(out);
    }

    @SneakyThrows
    @Command(name = "unesc-proto", description = "Unescape proto ByteString.")
    public void unescapeProto(@Parameters(arity = "1") String input) {
        TextFormat.unescapeBytes(input).writeTo(System.out);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    @Command(name = "parse-json")
    public void parseJson(@Parameters(arity = "1", description = "JSON file Path") String jsonPath,
                          @Option(names = "--mvel", description = "MVEL Expression") String mvelExp) {
        var gosn = new Gson();

        var rawJson = Files.readString(Path.of(jsonPath));
        var jsonMap = gosn.fromJson(rawJson, Map.class);

        if (nonNull(mvelExp)) {
            var imports = new HashMap<String, Object>();
            imports.put("unescProtoBytes", MVEL.getStaticMethod(TextFormat.class, "unescapeBytes", new Class[]{CharSequence.class}));

            var ce = (CompiledExpression) MVEL.compileExpression(mvelExp, imports);
            var result = MVEL.executeExpression(ce, jsonMap);
            System.out.println(result);
        } else {
            System.out.println(jsonMap);
        }
    }

    @Command
    public void info() throws IOException {
        log.info("DOER_HOME = {}", System.getenv().get("DOER_HOME"));
        log.info("DOER_CONFIG = {}", System.getenv().get(Doer.ENV_CONFIG));
        log.info("DOER_VCS_REF = {}", System.getenv().get("DOER_VCS_REF"));

        Utils.readResource(GitProps.GIT_PROPERTIES, br -> br.lines().forEach(log::info));
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
                                log.warn("can't delete: {} ({})", p, e.getMessage());
                            }
                        });
            }
        }
    }

    @Command
    public void rules(@Parameters(arity = "1..") List<File> rules,
                      @Option(names = "-f") Map<String, String> facts) {
        var gson = new Gson();
        var ruleFactory = new MVELRuleFactory(new YamlRuleDefinitionReader());
        var allFacts = new Facts();
        allFacts.put("log", log);
        facts.forEach((k, v) -> {
            if (v.endsWith(".json")) {
                try {
                    var factJson = Files.readString(Path.of(v));
                    var mapFact = gson.fromJson(factJson, Map.class);
                    allFacts.put(k, mapFact);

                } catch (IOException e) {
                    log.warn("parsing json fact {}", v, e);
                }
            } else {
                allFacts.put(k, v);
            }
        });

        var allRules = new Rules();
        rules.forEach(file -> {
            try {
                var ruleText = Files.readString(file.toPath());
                var rule = ruleFactory.createRule(new StringReader(ruleText));
                allRules.register(rule);

            } catch (Exception e) {
                log.warn("parsing rule {}", file, e);
            }
        });
        var rulesEngine = new DefaultRulesEngine();
        rulesEngine.fire(allRules, allFacts);
    }

    @Command(name = "log2es", description = "Emit json-formatted logs into Elastic Search.")
    @SuppressWarnings("unchecked")
    public void emitLogToElastic(
            @Parameters(arity = "1", description = "elasticsearch index") URI elastic,
            @Option(names = "--logFile", description = "Json formatted log file.") Path logFile,
            @Option(names = "--exec", description = "Logs from executable.") String exec,
            @Option(names = "-f", defaultValue = "source=doer", description = "additional properties: key=value") Map<String, String> fields) {
        final var gson = new Gson();

        var lines = new AtomicLong();
        try (var out = new HttpOutput(elastic.toString())) {
            out.open();

            Stream.<Supplier<BufferedReader>>of(
                            () -> {
                                try {
                                    return nonNull(logFile) ? Files.newBufferedReader(logFile) : null;
                                } catch (IOException e) {
                                    LOG.error("log file {}", logFile, e);
                                    return null;
                                }
                            },
                            () -> {
                                try {
                                    if (nonNull(exec)) {
                                        log.info("using exec: [{}]", exec);

                                        var is = Runtime.getRuntime().exec(exec).getInputStream();
                                        return new BufferedReader(new InputStreamReader(is));
                                    }
                                } catch (Exception e) {
                                    LOG.error("exec {}", exec, e);

                                }
                                return null;
                            },
                            () -> {
                                try {
                                    var clipboard = Clipboard.getString();
                                    LOG.debug("clipboard data: {}", clipboard);
                                    return new BufferedReader(new StringReader(clipboard));
                                } catch (Exception e) {
                                    LOG.error("using clipboard", e);
                                    return null;
                                }
                            })
                    .map(Supplier::get)
                    .map(Optional::ofNullable)
                    .flatMap(Optional::stream)
                    .findAny()
                    .ifPresentOrElse(src -> {
                        try (var br = src) {
                            br.lines()
                                    .filter(l -> l.startsWith("{"))
                                    .map(l -> {
                                        try {
                                            var js = gson.fromJson(l, Map.class);
                                            js.putAll(fields);
                                            return Optional.of(gson.toJson(js).getBytes());
                                        } catch (Exception e) {
                                            return Optional.<byte[]>empty();
                                        }
                                    })
                                    .flatMap(Optional::stream)
                                    .peek(l -> lines.incrementAndGet())
                                    .forEach(l -> out.emit("log", l));
                        } catch (IOException e) {
                            log.error("oops", e);
                            throw new HandledRuntimeException(e);
                        }
                    }, () -> log.info("noting to do"));

        } catch (Exception e) {
            log.error("oops", e);
            throw new HandledRuntimeException(e);
        }

        log.info("Total number of lines:  {}", lines.get());
    }
}
