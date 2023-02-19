package io.github.s7i.doer.pipeline;

import io.github.s7i.doer.Globals;
import io.github.s7i.doer.util.Mark;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumMap;
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

    enum PipelineKind {
        GRPC
    }

    private Map<PipelineKind, Supplier<PipeConnection>> factory = new EnumMap<>(PipelineKind.class);

    void init(Map<String, String> params) {
        var kind = PipelineKind.valueOf(params.get(DOER_PIPELINE).toUpperCase());
        switch (kind) {
            case GRPC:
                factory.put(kind, () -> new GrpcConnection(params.get(DOER_PIPELINE +".target")) );
                break;
        }
    }

    public PipeConnection connect(String name) {
        return factory.get(PipelineKind.GRPC).get();
    }

}
