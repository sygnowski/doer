package io.github.s7i.doer.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public interface ProtoDescriptorContainer {

    List<String> getDescriptorSet();

    default List<Path> getDescriptorsPaths() {
        return getDescriptorSet()
              .stream()
              .map(Paths::get)
              .collect(Collectors.toList());
    }

}
