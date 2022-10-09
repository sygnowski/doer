package io.github.s7i.doer;

import io.github.s7i.doer.domain.ConfigProcessor;
import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.github.s7i.doer.domain.output.OutputFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.github.s7i.doer.pipeline.Pipeline;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum Globals implements Context {
    INSTANCE;
    private final Map<String, Scope> scopeMap = new ConcurrentHashMap<>();

    @Getter
    List<Runnable> stopHooks = new ArrayList<>();
    @Getter
    KafkaFactory kafka = new KafkaFactory();
    @Getter
    Pipeline pipeline = new Pipeline();

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

        public Supplier<Map<String, String>> getParams() {
            if (System.getenv().containsKey(Doer.ENV_CONFIG)) {
                return new ConfigReader(params);
            }
            return params;
        }

        private OutputFactory outputFactory = new OutputFactory();

    }

    @RequiredArgsConstructor
    public static class ConfigReader implements Supplier<Map<String, String>> {

        private final Supplier<Map<String, String>> parent;

        Map<String, String> readParameters() {
            var path = Path.of(System.getenv(Doer.ENV_CONFIG));
            if (Files.exists(path)) {
                return new ConfigProcessor(path.toFile()).parseManifest().getParams();
            }
            return Collections.emptyMap();
        }

        @Override
        public Map<String, String> get() {
            var p = new HashMap<String, String>();
            p.putAll(readParameters());
            p.putAll(parent.get());
            return p;
        }
    }

}
