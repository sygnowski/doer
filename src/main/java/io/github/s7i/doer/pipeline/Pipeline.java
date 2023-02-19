package io.github.s7i.doer.pipeline;

import io.github.s7i.doer.Globals;
import io.github.s7i.doer.util.Mark;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class Pipeline {

    @Mark.Param
    public static final String DOER_PIPELINE = "doer.pipeline.backend";

    public static void initFrom(Supplier<Map<String, String>> params) {
        var p = params.get().entrySet()
                .stream()
                .filter(es -> es.getKey().startsWith(DOER_PIPELINE))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Globals.INSTANCE.getPipeline().init(p);
    }


    void init(Map<String, String> params) {

    }

    public PipeConnection connect(String name) {
        return null;
    }

}
