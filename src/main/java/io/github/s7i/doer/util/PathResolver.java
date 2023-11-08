package io.github.s7i.doer.util;

import io.github.s7i.doer.Globals;

import java.nio.file.Path;

public interface PathResolver {

    default Path resolvePath(String path) {
        var resolved = Globals.INSTANCE.getPropertyResolver().resolve(path);
        return Path.of(resolved);
    }

}
