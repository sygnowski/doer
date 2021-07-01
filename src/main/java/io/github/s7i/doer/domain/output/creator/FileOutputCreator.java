package io.github.s7i.doer.domain.output.creator;

import io.github.s7i.doer.domain.output.FileOutput;
import io.github.s7i.doer.domain.output.Output;
import java.nio.file.Path;

public interface FileOutputCreator extends OutputCreator {

    Path getStorageDir();

    default Output create() {
        return new FileOutput(getStorageDir());
    }
}
