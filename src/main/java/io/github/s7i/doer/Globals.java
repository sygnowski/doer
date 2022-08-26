package io.github.s7i.doer;

import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.github.s7i.doer.domain.output.OutputFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
public enum Globals implements Context {
    INSTANCE;
    private final Map<String, Scope> scopeMap = new ConcurrentHashMap<>();

    @Getter
    List<Runnable> stopHooks = new ArrayList<>();
    @Getter
    KafkaFactory kafka = new KafkaFactory();

    public Scope getScope() {

        String contextUid = ThreadLocal.withInitial(() -> Thread.currentThread().getName()).get();
        return scopeMap.computeIfAbsent(contextUid, uid -> {
            log.debug("new context {}", uid);
            return new Scope();
        });
    }


    @Getter
    public static class Scope {

        @Setter
        private Supplier<Path> root = () -> Path.of(".");
        @Setter
        private Supplier<Map<String, String>> params = Map::of;

        private OutputFactory outputFactory = new OutputFactory();

    }

}
