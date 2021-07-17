package io.github.s7i.doer.domain.output;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class FileOutput implements Output {

    final Path dest;

    @Override
    public void open() {
        requireNonNull(dest);
        try {
            Files.createDirectories(dest);
        } catch (IOException e) {
            log.error("{}", e);
        }
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public void emit(Load load) {
        var resource = load.getResource();
        var data = load.getData();

        final var location = dest.resolve(resource + ".txt");
        if (!Files.exists(location)) {
            try {
                log.debug("write to file: {}", location);
                Files.write(location, data, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (IOException | RuntimeException e) {
                log.error("{}", e);
            }
        }
    }
}
