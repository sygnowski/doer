package io.github.s7i.meshtastic;

public record Options(
      int socketTimeoutRetry,
      int errorRetry,
      int delayMillis,
      int smallDelay
) {

    public static final String MESHTASTIC_SOCKET_TIMEOUT_RETRY = "meshtastic.socket.timeout.retry";
    public static final String MESHTASTIC_SOCKET_RETRY = "meshtastic.socket.retry";
    public static final String MESHTASTIC_SOCKET_TIMEOUT_DELAY = "meshtastic.socket.timeout.delay";
    public static final String DELAY_SMALL = "delay.small";

    public static Options fromSystem() {

        return new Options(
              Integer.getInteger(MESHTASTIC_SOCKET_TIMEOUT_RETRY, 123),
              Integer.getInteger(MESHTASTIC_SOCKET_RETRY, 3),
              Integer.getInteger(MESHTASTIC_SOCKET_TIMEOUT_DELAY, 500),
              Integer.getInteger(DELAY_SMALL, 100)
        );

    }

}
