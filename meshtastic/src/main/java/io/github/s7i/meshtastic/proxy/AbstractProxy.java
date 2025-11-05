package io.github.s7i.meshtastic.proxy;

import static io.github.s7i.meshtastic.MeshtasticStream.HEADER_LEN;
import static io.github.s7i.meshtastic.MeshtasticStream.MAX_TO_FROM_RADIO_SIZE;

import java.nio.ByteBuffer;

public abstract class AbstractProxy implements StreamProxy {

    private final ByteFlow byteFlow;

    protected AbstractProxy() {
        this.byteFlow = initByteFlow();
    }

    protected abstract ByteFlow initByteFlow();

    private final ByteBuffer txBuff = ByteBuffer.allocate(MAX_TO_FROM_RADIO_SIZE + HEADER_LEN);

    @Override
    public void rx(int dat) {
        txBuff.put((byte) dat);
    }

    @Override
    public void rxFlush() {
        byte[] buff = extractRemaining(txBuff);
        byteFlow.outbound(buff);
    }


    @Override
    public byte[] toTx() {
        return byteFlow.inbound();
    }

    public static byte[] extractRemaining(ByteBuffer bb) {
        bb.flip();

        byte[] buff = new byte[bb.remaining()];
        bb.get(buff);

        bb.compact();
        return buff;
    }
}
