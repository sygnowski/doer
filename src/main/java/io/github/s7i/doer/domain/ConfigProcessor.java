package io.github.s7i.doer.domain;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.command.YamlParser;
import io.github.s7i.doer.manifest.ConfigManifest;
import lombok.RequiredArgsConstructor;

import java.io.File;

@RequiredArgsConstructor
public class ConfigProcessor implements YamlParser {

    private final File yaml;

    @Override
    public File getYamlFile() {
        return yaml;
    }

    public void processConfig() {
        new Context.Initializer(Context.InitialParameters.builder()
                .workDir(yaml.toPath().toAbsolutePath().getParent())
                .params(parseManifest().getParams())
                .build());

    }
    public ConfigManifest parseManifest() {
        var manifest = parseYaml(ConfigManifest.class);
        return manifest;
    }
}
