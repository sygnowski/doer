package io.github.s7i.doer;

import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.github.s7i.doer.domain.output.OutputFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum Globals {
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

    @FieldDefaults(level = AccessLevel.PUBLIC)
    @Getter
    public class Scope {

        Supplier<Path> root;
        Supplier<Map<String, String>> params;

        OutputFactory outputFactory = new OutputFactory();

    }

}
