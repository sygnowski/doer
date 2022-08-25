package io.github.s7i.doer;

import io.github.s7i.doer.command.KafkaFeeder;
import io.github.s7i.doer.command.ProtoProcessor;
import io.github.s7i.doer.command.Rocks;
import io.github.s7i.doer.command.dump.KafkaDump;
import io.github.s7i.doer.command.file.ReplaceInFile;
import io.github.s7i.doer.command.util.CommandManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.Arrays;

@Command(name = "doer", description = "let's do big things...", subcommands = {
      KafkaFeeder.class,
      KafkaDump.class,
      ProtoProcessor.class,
      Rocks.class,
      ReplaceInFile.class})
public class Doer {

    static final Logger CONSOLE = LoggerFactory.getLogger("doer.console");
    public static final String FLAGS = "doer.flags";
    public static final String FLAG_USE_TRACING = "trace";
    public static final String FLAG_SEND_AND_FORGET = "send-and-forget";
    public static final String FLAG_RAW_DATA = "raw-data";
    public static final int EC_QUIT = 7;

    public static Logger console() {
        return CONSOLE;
    }

    public static void main(String[] args) {
        boolean onlyCommandManifests = args.length > 0 && Arrays.stream(args)
              .allMatch(a -> a.endsWith(".yml") || a.endsWith(".yaml"));

        var command = onlyCommandManifests
              ? new CommandManifest()
              : new Doer();

        new CommandLine(command)
              .setCaseInsensitiveEnumValuesAllowed(true)
              .execute(args);
    }
}
