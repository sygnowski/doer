package io.github.s7i.doer.flow;

import java.util.Map;
import lombok.Getter;

public class Variant {

    @Getter
    Map<String, Iterable<String>> options;

    public Variant(Map<String, Iterable<String>> options) {
        this.options = options;
    }
}
