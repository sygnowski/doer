package io.github.s7i.meshtastic.proxy;


public interface StreamProxy {

    void rx(int dat);

    void rxFlush();

    byte[] toTx();
}
