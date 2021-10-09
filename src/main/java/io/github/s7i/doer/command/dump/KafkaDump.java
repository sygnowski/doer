package io.github.s7i.doer.command.dump;


import static io.github.s7i.doer.Doer.console;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.Context.InitialParameters;
import io.github.s7i.doer.command.YamlParser;
import io.github.s7i.doer.domain.kafka.dump.KafkaWorker;
import io.github.s7i.doer.manifest.dump.Dump;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "kdump")
@Slf4j
public class KafkaDump implements Runnable, YamlParser {

    public static KafkaDump createCommandInstance(File yaml) {
        var cmd = new KafkaDump();
        cmd.yaml = yaml;
        return cmd;
    }

    @Option(names = {"-y", "-yaml"}, defaultValue = "dump.yml")
    protected File yaml;

    @Override
    public File getYamlFile() {
        if (!yaml.exists()) {
            throw new IllegalStateException("missing file with definition of kafka-dump.yml");
        }
        return yaml;
    }

    @Override
    public void run() {
        var config = parseYaml(Dump.class);
        var workDir = yaml.toPath().toAbsolutePath().getParent();
        
        new Context.Initializer(InitialParameters.builder()
              .workDir(workDir)
              .params(config.getParams())
              .build());

        console().info("Start dumping from Kafka");
        new KafkaWorker(config).pool();
    }

}
