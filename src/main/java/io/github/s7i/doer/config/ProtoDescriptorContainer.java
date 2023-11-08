package io.github.s7i.doer.config;

import io.github.s7i.doer.util.PathResolver;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public interface ProtoDescriptorContainer extends PathResolver {

    List<String> getDescriptorSet();

    default List<Path> getDescriptorsPaths() {
        return getDescriptorSet()
              .stream()
              .map(this::resolvePath)
              .collect(Collectors.toList());
    }

}
