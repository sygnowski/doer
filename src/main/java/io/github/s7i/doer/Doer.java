package io.github.s7i.doer;

import io.github.s7i.doer.command.KafkaFeeder;
import io.github.s7i.doer.command.ProtoProcessor;
import io.github.s7i.doer.command.Rocks;
import io.github.s7i.doer.command.dump.KafkaDump;
import io.github.s7i.doer.command.file.ReplaceInFile;
import io.github.s7i.doer.command.util.CommandManifest;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "doer", description = "let's do big things...", subcommands = {
      KafkaFeeder.class,
      KafkaDump.class,
      ProtoProcessor.class,
      Rocks.class,
      ReplaceInFile.class})
public class Doer {

    public static final Logger CONSOLE = LoggerFactory.getLogger("doer.console");

    public static void main(String[] args) {
        boolean onlyCommandManifests = args.length > 0 && Arrays.stream(args)
              .allMatch(a -> a.endsWith(".yml") || a.endsWith(".yaml"));

        var command = onlyCommandManifests
              ? new CommandManifest()
              : new Doer();

        new CommandLine(command)
              .execute(args);
    }
}
