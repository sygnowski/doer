package io.github.s7i.meshtastic.proxy;

public interface ByteFlow {

    byte[] NO_DATA = new byte[0];

    void outbound(byte[] data);

    default byte[] inbound() {
        return NO_DATA;
    }

}
