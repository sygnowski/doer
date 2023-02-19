package io.github.s7i.doer.pipeline;

import io.github.s7i.doer.Globals;
import io.github.s7i.doer.util.Mark;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Slf4j
public class Pipeline {

    @Mark.Param
    public static final String DOER_PIPELINE = "doer.pipeline.backend";

    public static void initFrom(Supplier<Map<String, String>> params) {
        var setup = params.get().entrySet()
                .stream()
                .filter(es -> es.getKey().startsWith(DOER_PIPELINE))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!setup.isEmpty()) {
            Globals.INSTANCE.getPipeline().init(setup);
        }
    }

    enum PipelineKind {
        GRPC
    }

    private Map<PipelineKind, BackendFactory> register = new EnumMap<>(PipelineKind.class);

    void init(Map<String, String> params) {
        var backend = requireNonNull(params.get(DOER_PIPELINE), DOER_PIPELINE).toUpperCase();

        var kind = PipelineKind.valueOf(backend);
        switch (kind) {
            case GRPC:
                register.put(kind, () -> {
                    var c = new GrpcConnection(params.get(DOER_PIPELINE +".target"));
                    Globals.INSTANCE.addStopHook(c::closeSafe);
                    return c;
                });
                break;
        }
    }

    public PipeConnection connect(String name) {
        return register.get(PipelineKind.GRPC).create();
    }

    public boolean isEnabled() {
        return !register.isEmpty();
    }

}
