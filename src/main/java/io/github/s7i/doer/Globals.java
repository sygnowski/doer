package io.github.s7i.doer;

import io.github.s7i.doer.domain.ConfigProcessor;
import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.github.s7i.doer.domain.output.OutputFactory;
import io.github.s7i.doer.pipeline.Pipeline;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * <pre>
 *     "Dependency injection is a kind of globals with configuration."
 * </pre>
 */
@Slf4j
public enum Globals implements Context {
    INSTANCE;
    private final Map<String, Scope> scopeMap = new ConcurrentHashMap<>();


    private final AtomicReference<List<Runnable>> stopHooks = new AtomicReference<>(new ArrayList<>());

    private final AtomicReference<KafkaFactory> kafka = new AtomicReference<>(new KafkaFactory());

    private final AtomicReference<Pipeline> refPipeline = new AtomicReference<>(new Pipeline());

    public List<Runnable> stopHooks() {
        return stopHooks.get();
    }

    public KafkaFactory kafka() {
        return kafka(null);
    }

    public KafkaFactory kafka(KafkaFactory kafkaFactory) {
        if (kafkaFactory != null) {
            kafka.set(kafkaFactory);
            return kafkaFactory;
        }
        return kafka.get();
    }

    public Pipeline pipeline() {
        return pipeline(null);
    }

    public Pipeline pipeline(Pipeline pipeline) {
        if (pipeline != null) {
            refPipeline.set(pipeline);
            return pipeline;
        }
        return refPipeline.get();
    }

    public Scope getScope() {

        String contextUid = InheritableThreadLocal.withInitial(() -> Thread.currentThread().getName()).get();
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

        public OutputFactory outputFactory() {
            return refOutputFactory.get();
        }

        public OutputFactory outputFactory(OutputFactory outputFactory) {
            if (outputFactory != null) {
                this.refOutputFactory.set(outputFactory);
                return outputFactory;
            }
            return this.refOutputFactory.get();
        }

        private final AtomicReference<OutputFactory> refOutputFactory = new AtomicReference<>(new OutputFactory());

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
