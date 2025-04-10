package io.github.s7i.doer.domain.proto;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.DoerException;
import org.slf4j.LoggerFactory;

public interface ProtoToJsonWrite {

    static ProtoToJsonWrite from(Context context) {
        var decoderClass = context.getParams().getOrDefault("doer.proto.decoder.class", "");
        if ("".equals(decoderClass)) {
            return new SimpleJsonWriter();
        }
        try {
            return (ProtoToJsonWrite) Class.forName(decoderClass).getConstructor().newInstance();
        } catch (Exception e) {
            throw new DoerException(e);
        }
    }


    default String toProto(byte[] data) {
        LoggerFactory.getLogger(ProtoToJsonWrite.class).warn("NOOP");
        return "{}";
    }

}
