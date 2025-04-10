package io.github.s7i.doer.domain.proto;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.DoerException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.LoggerFactory;

public interface ProtoToJsonWrite {

    Map<Class<?>, Supplier<ProtoToJsonWrite>> factories = new ConcurrentHashMap<>(1);

    static ProtoToJsonWrite from(Context context) {
        var decoderClass = context.getParams().getOrDefault("doer.proto.decoder.class", "");
        if ("".equals(decoderClass)) {
            return new SimpleJsonWriter();
        }
        try {
            var clazz = Class.forName(decoderClass);
            factories.computeIfAbsent(clazz, (type) -> () -> {
                try {
                    return (ProtoToJsonWrite) type.getConstructor().newInstance();
                } catch (Exception e) {
                    throw new DoerException(e);
                }
            });
            return factories.get(clazz).get();
        } catch (Exception e) {
            throw new DoerException(e);
        }
    }


    default String toProto(byte[] data) {
        LoggerFactory.getLogger(ProtoToJsonWrite.class).warn("NOOP");
        return "{}";
    }

}
