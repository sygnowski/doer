package io.github.s7i.doer;

import io.github.s7i.doer.Doer.Help;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.RunLast;


@Command(subcommands = {KafkaFeeder.class, KafkaDump.class, ProtoProcessor.class, Help.class})
public class Doer {

    @Command(name = "help")
    public static class Help implements Runnable {

        @Override
        public void run() {
            System.out.println("help not yet implemented");
        }
    }

    public static void main(String[] args) {
        new CommandLine(new Doer())
              .setExecutionStrategy(new RunLast())
              .execute(args);
    }
}
