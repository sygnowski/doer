package io.github.s7i.doer.domain.proto;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.UnknownFieldSet;
import io.github.s7i.doer.DoerException;

public class SimpleJsonWriter implements ProtoToJsonWriteWithDescriptor {

    @Override
    public String toProto(byte[] data) {
        try {
            var msg = UnknownFieldSet.parseFrom(data);
            var x = new JsonObject();
            x.addProperty("raw", msg.toString());
            return new Gson().toJson(x);
        } catch (InvalidProtocolBufferException e) {
            throw new DoerException(e);
        }
    }
}
