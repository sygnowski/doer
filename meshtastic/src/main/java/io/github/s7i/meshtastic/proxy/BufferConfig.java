package io.github.s7i.meshtastic.proxy;

public class BufferConfig {

    public static final int BUFFER_SMALL = Integer.getInteger("buffer.small", 1024);
    public static final int BUFF_TX = 1024 * BufferConfig.BUFFER_SMALL;
    public static final int BUFF_RX = 100 * BufferConfig.BUFFER_SMALL;
}
