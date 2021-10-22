package io.github.s7i.doer.command.dump;


import io.github.s7i.doer.command.YamlParser;
import io.github.s7i.doer.domain.kafka.dump.DumpBuilder;
import io.github.s7i.doer.flow.Task;
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
        var workDir = yaml.toPath().toAbsolutePath().getParent();
        new DumpBuilder()
              .setManifest(parseYaml(Dump.class))
              .setWorkDir(workDir)
              .build()
              .getTasks()
              .forEach(Task::execute);
    }

}
