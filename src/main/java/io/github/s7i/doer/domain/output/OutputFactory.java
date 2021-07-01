package io.github.s7i.doer.domain.output;

import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.domain.output.creator.OutputCreator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class OutputFactory {

    private final Map<OutputKind, OutputCreator> creatorMap = new EnumMap<>(OutputKind.class);

    public OutputFactory() {
        register(OutputKind.CONSOLE, ConsoleOutput::new);
    }

    public OutputFactory register(OutputKind kind, OutputCreator creator) {
        creatorMap.put(kind, creator);
        return this;
    }

    public Optional<Output> create(OutputKind kind) {
        if (!creatorMap.containsKey(kind)) {
            return Optional.empty();
        }
        return Optional.ofNullable(creatorMap.get(kind).create());
    }

    public Optional<Output> resolve(OutputKindResolver resolver) {
        requireNonNull(resolver, "resolver");
        return create(resolver.resolveOutputKind());
    }
}
