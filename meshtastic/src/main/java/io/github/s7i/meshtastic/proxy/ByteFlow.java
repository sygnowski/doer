package io.github.s7i.meshtastic.proxy;

public interface ByteFlow {


    void outbound(byte[] data);

    byte[] inbound();

}
