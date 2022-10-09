package io.github.s7i.doer.command;

import io.github.s7i.doer.manifest.ManifestException;
import lombok.SneakyThrows;
import picocli.CommandLine.Option;

import java.io.File;

import static java.util.Objects.isNull;

public abstract class ManifestFileCommand extends Command implements YamlParser {

    public static class Builder {
        @SneakyThrows
        public static <T extends ManifestFileCommand> T fromManifestFile(Class<T> clazz, File manifest) {
            T instance = clazz.getConstructor().newInstance();
            instance.yaml = manifest;
            return instance;
        }
    }

    @Option(names = {"-y", "-yaml"})
    protected File yaml;

    @Override
    public File getYamlFile() {
        if (isNull(yaml)) {
            yaml = getDefaultManifestFile();
        }
        if (!yaml.exists()) {
            throw new ManifestException("missing manifest file: " + yaml);
        }
        return yaml;
    }

    protected abstract File getDefaultManifestFile();
}
