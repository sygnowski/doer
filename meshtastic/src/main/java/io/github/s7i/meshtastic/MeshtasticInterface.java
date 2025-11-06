package io.github.s7i.meshtastic;

import java.util.function.Consumer;

public interface MeshtasticInterface {

    void connect();

    void disconnect();

    void sendToRadio(byte[] toRadio);

    void handleFromRadio(Consumer<byte[]> fromRadio);
}
