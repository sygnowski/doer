package io.github.s7i.meshtastic;

import static io.github.s7i.meshtastic.MeshtasticStream.HEADER_LEN;
import static io.github.s7i.meshtastic.MeshtasticStream.MAX_TO_FROM_RADIO_SIZE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class StreamProxy {

    OutputStream os;
    InputStream is;

    private final ByteBuffer txBuff = ByteBuffer.allocate(MAX_TO_FROM_RADIO_SIZE + HEADER_LEN);

    public void rx(int dat) {
        txBuff.put((byte) dat);
    }

    public void rxFlush() {
        txBuff.flip();

        byte[] buff = new byte[txBuff.remaining()];
        txBuff.get(buff);

        txBuff.compact();

        try {
            os.write(buff);
            os.flush();
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
    }


    public byte[] toTx() {
        return null;
    }
}
