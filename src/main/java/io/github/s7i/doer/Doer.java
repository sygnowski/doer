package io.github.s7i.doer;

import io.github.s7i.doer.command.GrpcHealth;
import io.github.s7i.doer.command.Helix;
import io.github.s7i.doer.command.KafkaFeeder;
import io.github.s7i.doer.command.Meshtastic;
import io.github.s7i.doer.command.MqttCommand;
import io.github.s7i.doer.command.ProtoProcessor;
import io.github.s7i.doer.command.Rocks;
import io.github.s7i.doer.command.ZooSrv;
import io.github.s7i.doer.command.dump.KafkaDump;
import io.github.s7i.doer.command.util.CommandManifest;
import io.github.s7i.doer.command.util.HmacCommand;
import io.github.s7i.doer.command.util.Misc;
import io.github.s7i.doer.domain.ServiceEntrypoint;
import io.github.s7i.doer.domain.grpc.GrpcServer;
import io.github.s7i.doer.pipeline.PipelineService;
import io.github.s7i.doer.util.Banner;
import io.github.s7i.doer.util.GitProps;
import java.io.IOException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.util.Arrays;

@Command(name = "doer", description = "let's do big things...", subcommands = {
      KafkaFeeder.class,
      KafkaDump.class,
      ProtoProcessor.class,
      Helix.class,
      Rocks.class,
      GrpcHealth.class,
      ZooSrv.class,
      PipelineService.class,
      MqttCommand.class,
      Misc.class,
      HmacCommand.class,
      Meshtastic.class
})
public class Doer implements Runnable, Banner {

    public static final String DOER_CONSOLE = "doer.console";
    static final Logger CONSOLE = LoggerFactory.getLogger(DOER_CONSOLE);
    public static final String FLAGS = "doer.flags";
    public static final String FLAG_USE_TRACING = "trace";
    public static final String FLAG_SEND_AND_FORGET = "send-and-forget";
    public static final String FLAG_RAW_DATA = "raw-data";
    public static final String FLAG_DRY_RUN = "dry-run";
    public static final String FLAG_FAIL_FAST = "fail-fast";
    public static final int EC_INVALID_USAGE = 1;
    public static final int EC_QUIT = 7;
    public static final int EC_ERROR = 4;

    public static final String ENV_CONFIG = "DOER_CONFIG";

    public static Logger console() {
        return CONSOLE;
    }

    @CommandLine.Option(names = {"-v", "--version"})
    private boolean showVersion;

    @Command(name = "srv-grpc")
    public int service(int port) {
        try {
            new GrpcServer(port, new ServiceEntrypoint()).startServer();
            return 0;
        } catch (IOException e) {
            console().error("grpc-server", e);
            return EC_ERROR;
        }
    }

    @Override
    public void run() {
        printBanner();
        if (showVersion) {
            console().info("version: {}", new GitProps());
        } else {
            new CommandLine(Doer.class).usage(System.out);
        }
    }

    public static void main(String[] args) {
        boolean onlyCommandManifests = args.length > 0 && Arrays.stream(args)
              .allMatch(a -> a.endsWith(".yml") || a.endsWith(".yaml"));

        var command = onlyCommandManifests
              ? new CommandManifest()
              : new Doer();

        var exitCode = new CommandLine(command)
              .setCaseInsensitiveEnumValuesAllowed(true)
              .execute(args);

        System.exit(exitCode);
    }
}
