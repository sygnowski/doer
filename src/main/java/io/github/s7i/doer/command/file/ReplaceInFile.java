package io.github.s7i.doer.command.file;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "replace-in-file", aliases = "rif")
@Slf4j(topic = "doer.console")
public class ReplaceInFile implements Callable<Integer> {

    public static final String SHADOW = "shadow";

    @Option(names = "-f", required = true)
    private List<File> files;

    @Option(names = "-e")
    private List<String> extension;

    @Option(names = "-r", required = true)
    private Map<String, String> replacements;

    @Option(names = "-k")
    private boolean keepShadow;

    @Override
    public Integer call() throws Exception {
        if (isNull(replacements)) {
            return -1;
        }
        if (isNull(files)) {
            return -1;
        }
        files().forEach(this::replaceInFile);
        return 0;
    }

    @SneakyThrows
    private Stream<Path> files() {
        List<Path> fileList = new ArrayList<>();
        if (nonNull(files)) {
            for (var f : files) {
                if (!f.exists()) {
                    continue;
                }
                var path = f.toPath();

                if (f.isDirectory()) {
                    Files.list(path)
                          .filter(this::matchCriteria)
                          .forEach(fileList::add);
                } else {
                    fileList.add(path);
                }
            }
        }
        return fileList.stream();
    }

    private boolean matchCriteria(Path path) {
        if (nonNull(extension)) {
            var name = path.getFileName().toString();
            return extension.stream().anyMatch(name::endsWith);
        }
        return true;
    }

    private void replaceInFile(Path file) {
        try {
            var shadow = file.getParent().resolve(file.getFileName().toString() + "." + SHADOW);
            try (var w = new PrintWriter(Files.newOutputStream(shadow, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                Files.lines(file)
                      .map(this::replaceLine)
                      .forEach(w::println);
            }
            if (!keepShadow) {
                Files.move(shadow, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ioe) {
            log.error("replace", ioe);
        }
    }

    private String replaceLine(String line) {
        var newLine = line;
        for (var e : replacements.entrySet()) {
            newLine = newLine.replaceAll(e.getKey(), e.getValue());
        }
        return newLine;

    }
}
