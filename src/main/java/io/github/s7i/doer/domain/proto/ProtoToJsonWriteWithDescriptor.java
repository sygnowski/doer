package io.github.s7i.doer.domain.proto;

import com.google.protobuf.Descriptors.Descriptor;

public interface ProtoToJsonWriteWithDescriptor extends ProtoToJsonWrite {

    default String toProto(Descriptor descriptor, byte[] data) {
        return toProto(data);
    }

    default String toJson(Descriptor descriptor, byte[] data, boolean safe) {
        return toProto(descriptor, data);
    }

}
