package io.github.s7i.meshtastic.proxy;

import static io.github.s7i.meshtastic.proxy.BufferConfig.BUFFER_SMALL;
import static io.github.s7i.meshtastic.proxy.BufferConfig.BUFF_RX;
import static io.github.s7i.meshtastic.proxy.BufferConfig.BUFF_TX;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BufferProxy extends AbstractProxy implements ByteFlow {

    public static final Logger LOGGER = LoggerFactory.getLogger(BufferProxy.class);


    private final Object txLock = new Object();
    private final ReentrantLock rxLock = new ReentrantLock();

    private final ByteBuffer tx = ByteBuffer.allocate(Integer.getInteger("buff.tx", BUFF_TX));
    private final ByteBuffer rx = ByteBuffer.allocate(Integer.getInteger("buff.rx", BUFF_RX));

    @Override
    protected ByteFlow initByteFlow() {
        return this;
    }

    public void waitForDataForTx() {
        synchronized (txLock) {
            try {
                txLock.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.debug("wait for tx end for thread: {}", Thread.currentThread().getName());
    }

    @Override
    public void outbound(byte[] data) {
        synchronized (txLock) {

            if (tx.remaining() < data.length) {
                LOGGER.debug("discarding TX BUFFER due no proxy transfer");
                tx.clear();
            }

            tx.put(data);

            txLock.notifyAll();
        }
    }

    @Override
    public byte[] inbound() {
        rxLock.lock();
        try {
            return extractRemaining(rx);
        } finally {
            rxLock.unlock();
        }
    }

    public void doTx(Consumer<ByteBuffer> onTx) {
        if (tx.position() == 0) {
            return;
        }
        synchronized (txLock) {
            tx.flip();
            onTx.accept(tx);
            tx.compact();
        }
    }

    public void doRx(Consumer<ByteBuffer> onRx) {
        rxLock.lock();
        try {
            if (rx.remaining() < BUFFER_SMALL) {
                LOGGER.debug("discarding RX BUFFER");
                rx.clear();
            }
            onRx.accept(rx);
        } finally {
            rxLock.unlock();
        }
    }
}
