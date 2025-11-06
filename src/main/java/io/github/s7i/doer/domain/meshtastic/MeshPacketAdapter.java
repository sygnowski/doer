package io.github.s7i.doer.domain.meshtastic;

import io.github.s7i.doer.domain.proto.ProtoToJsonWriteWithDescriptor;
import io.github.s7i.meshtastic.Proto;

public class MeshPacketAdapter implements ProtoToJsonWriteWithDescriptor {

    @Override
    public String toProto(byte[] data) {
        return Proto.INSTANCE.asJsonTextPacket(data);
    }
}
