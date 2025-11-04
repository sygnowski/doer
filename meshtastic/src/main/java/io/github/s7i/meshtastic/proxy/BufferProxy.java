package io.github.s7i.meshtastic.proxy;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class BufferProxy extends AbstractProxy implements ByteFlow {

    private final Object txLock = new Object();
    private final Object rxLock = new Object();

    private final ByteBuffer tx = ByteBuffer.allocate(Integer.getInteger("buff.tx", 1024 * 1024));
    private final ByteBuffer rx = ByteBuffer.allocate(Integer.getInteger("buff.rx", 1024 * 1024));

    @Override
    protected ByteFlow initByteFlow() {
        return this;
    }

    @Override
    public void outbound(byte[] data) {
        synchronized (txLock) {
            tx.put(data);
        }
    }

    public void doTx(Consumer<ByteBuffer> onTx) {
        synchronized (txLock) {
            tx.flip();
            onTx.accept(tx);
            tx.compact();
        }
    }
}
