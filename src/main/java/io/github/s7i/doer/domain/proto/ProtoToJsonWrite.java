package io.github.s7i.doer.domain.proto;

import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.DoerException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.LoggerFactory;

public interface ProtoToJsonWrite {

    class LazyFactory<T extends ProtoToJsonWrite> {

        final AtomicReference<T> ref = new AtomicReference<>();
        final Supplier<T> supplier;

        LazyFactory(Supplier<T> ref) {
            this.supplier = ref;
        }

        T get() {
            ref.compareAndSet(null, supplier.get());
            return ref.get();
        }
    }

    Map<Class<?>, LazyFactory<ProtoToJsonWrite>> factories = new ConcurrentHashMap<>(1);

    static ProtoToJsonWrite from(Context context) {
        try {
            Class<?> clazz;
            var decoderClass = context.getParams().getOrDefault("doer.proto.decoder.class", "");
            if ("".equals(decoderClass)) {
                clazz = SimpleJsonWriter.class;
            } else {
                clazz = Class.forName(decoderClass);
            }
            factories.computeIfAbsent(clazz, (type) -> new LazyFactory<>(() -> {
                try {
                    return (ProtoToJsonWrite) type.getConstructor().newInstance();
                } catch (Exception e) {
                    throw new DoerException(e);
                }
            }));
            return requireNonNull(factories.get(clazz).get(), "missing");
        } catch (Exception e) {
            throw new DoerException(e);
        }
    }

    default String toProto(byte[] data) {
        LoggerFactory.getLogger(ProtoToJsonWrite.class).warn("NOOP");
        return "{}";
    }
}
