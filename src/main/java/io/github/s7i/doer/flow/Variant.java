package io.github.s7i.doer.flow;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import lombok.Getter;

public class Variant {

    @Getter
    Map<String, Iterable<String>> options;

    public Variant(Map<String, Iterable<String>> options) {
        requireNonNull(options, "missing options");
        this.options = options;
    }
}
