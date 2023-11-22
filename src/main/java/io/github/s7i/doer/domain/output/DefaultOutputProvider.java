package io.github.s7i.doer.domain.output;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.util.Mark;

public interface DefaultOutputProvider {
    @Mark.Param
    String DOER_OUTPUT = "doer.output";

    default Output getDefaultOutput(Context context) {
        return context.getParams().entrySet().stream()
                .filter(e -> e.getKey().equals(DOER_OUTPUT))
                .map(def -> context.buildOutput(def::getValue))
                .findFirst()
                .orElseGet(ConsoleOutput::new);
    }
}
