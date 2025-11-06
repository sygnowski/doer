package io.github.s7i.doer.domain.output;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.util.Mark;
import org.slf4j.LoggerFactory;

public interface DefaultOutputProvider {
    @Mark.Param
    String DOER_OUTPUT = "doer.output";

    default Output getDefaultOutput(Context context) {
        return context.getParams().entrySet().stream()
                .filter(e -> e.getKey().equals(DOER_OUTPUT))
                .map(def -> {
                    var o = context.buildOutput(def::getValue);
                    context.addStopHook(() -> {
                        try {
                            o.close();
                        } catch (Exception e) {
                            LoggerFactory.getLogger(DefaultOutputProvider.class).warn("shutdown hook", e);
                        }
                    });
                    return o;
                })
                .findFirst()
                .orElseGet(ConsoleOutput::new);
    }
}
