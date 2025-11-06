package io.github.s7i.meshtastic;

import com.geeksville.mesh.MeshProtos.FromRadio;
import com.geeksville.mesh.MeshProtos.FromRadio.PayloadVariantCase;
import com.geeksville.mesh.MeshProtos.Heartbeat;
import com.geeksville.mesh.MeshProtos.ToRadio;
import com.google.protobuf.InvalidProtocolBufferException;
import io.github.s7i.meshtastic.proxy.StreamProxy;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeshtasticStream {

    public static final Logger LOGGER = LoggerFactory.getLogger(MeshtasticStream.class);

    public static final int START1 = 0x94;
    public static final int START2 = 0xC3;
    public static final int HEADER_LEN = 4;
    public static final int NODELESS_WANT_CONFIG_ID = 69420;
    public static final int MAX_TO_FROM_RADIO_SIZE = 512;
    public static final int LIMIT = 10;
    public static final String SP_DROP_TX = "drop.tx";

    private final InputStream is;
    private final OutputStream os;

    private final ByteBuffer rxPacket = ByteBuffer.allocate(MAX_TO_FROM_RADIO_SIZE).mark();
    private final AtomicReference<Consumer<FromRadio>> fromRadioHandler = new AtomicReference<>();
    private final AtomicReference<Runnable> onRxStop = new AtomicReference<>();
    private final ThreadGroup tg;
    private final ArrayBlockingQueue<FromRadio> pool;
    private final Thread[] threads;
    private final Options options;
    private int queueFree = Integer.MAX_VALUE;
    private StreamProxy proxy;
    private final ReentrantLock sendLock = new ReentrantLock();
    private final Boolean dropTx = Boolean.getBoolean(SP_DROP_TX);


    public MeshtasticStream(InputStream is, OutputStream os) {
        this(is, os, Options.fromSystem());
    }


    public MeshtasticStream(InputStream is, OutputStream os, Options options) {
        this.options = options;
        this.is = is;
        this.os = os;

        pool = new ArrayBlockingQueue<>(1000);
        tg = new ThreadGroup("Meshtastic Radio");

        threads = new Thread[]{
              new Thread(tg, this::handleRadioRx, "FromRadio"),
              new Thread(tg, this::handleHeartBeat, "HeartBeat"),
              new Thread(tg, this::handlePool, "FromRadio Fetcher")
        };
    }

    public void setProxy(StreamProxy proxy) {
        this.proxy = proxy;
    }


    public Options getOptions() {
        return options;
    }

    public void setHandler(Consumer<FromRadio> fromRadioConsumer) {
        fromRadioHandler.set(fromRadioConsumer);
    }

    public void onStop(Runnable onStop) {
        onRxStop.set(onStop);
    }

    public void send(ToRadio toSend) {
        if (queueFree <= 0) {
            throw new RuntimeException("too many to send");
        }
        sendToRadio(toSend.toByteArray());
    }

    private void sendToRadio(byte[] data) {
        if (dropTx) {
            LOGGER.debug("dropping tx data");
            return;
        }
        var len = data.length;

        var header = ByteBuffer.allocate(HEADER_LEN)
              .put((byte) START1)
              .put((byte) START2)
              .put((byte) ((len >> 8) & 0xFF))
              .put((byte) (len & 0xFF));

        try {
            sendLock.lockInterruptibly();
            try {
                os.write(header.array());
                os.write(data);
                os.flush();
            } catch (IOException e) {
                LOGGER.error("while sending to radio", e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            sendLock.unlock();
        }
    }

    public void startReadFromRadio(boolean withNodes) throws Exception {
        os.write(radioWakeup());
        os.flush();

        TimeUnit.MILLISECONDS.sleep(options.smallDelay());
        var configId = withNodes ? new Random().nextInt() : NODELESS_WANT_CONFIG_ID;
        var msg = ToRadio.newBuilder()
              .setWantConfigId(configId)
              .build();

        sendToRadio(msg.toByteArray());

        for (var thr : threads) {
            thr.setDaemon(true);
            thr.start();
        }
    }

    void handleRadioRx() {
        FromRadioReader reader = new FromRadioReader();
        LOGGER.debug("starting rx");
        int generalErr = 0;
        int errorCount = 0;
        while (!Thread.currentThread().isInterrupted()) {
            sendDataFromProxy();

            try {
                int c = is.read();

                if (proxy != null) {
                    proxy.rx(c);
                }

                errorCount = 0;

                if (c != -1) {
                    reader.readChar(c);
                } else {
                    LOGGER.warn("TCP EOF");
                    break;
                }
            } catch (SocketTimeoutException e) {
                if (++errorCount < options.socketTimeoutRetry()) {
                    nap();
                } else {
                    break;
                }
            } catch (IOException e) {
                if (++generalErr > options.errorRetry()) {
                    LOGGER.error("while reding from socket", e);
                    break;
                } else {
                    nap(generalErr);
                }
            }
        }
        LOGGER.debug("stopping rx");

        var onStop = onRxStop.get();
        if (onStop != null) {
            onStop.run();
        }
    }

    private void sendDataFromProxy() {
        if (dropTx) {
            LOGGER.debug("dropping tx data");
            return;
        }
        try {
            if (proxy != null) {
                var toTx = proxy.toTx();
                if (toTx.length > 0) {
                    sendLock.lock();
                    try {
                        os.write(toTx);
                        os.flush();
                    } finally {
                        sendLock.unlock();
                    }
                    LOGGER.debug("sent to radio from proxy, len: {}", toTx.length);
                }
            }
        } catch (Exception e) {
            LOGGER.error("sending data from proxy", e);
        }
    }

    private void nap() {
        nap(1L);
    }

    private void nap(long factor) {
        try {
            TimeUnit.MILLISECONDS.sleep(Math.min(factor, LIMIT) * options.delayMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    void handleHeartBeat() {
        byte[] hbData = ToRadio.newBuilder()
              .setHeartbeat(Heartbeat.newBuilder().build())
              .build().toByteArray();

        while (!Thread.currentThread().isInterrupted()) {
            sendToRadio(hbData);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void handlePool() {
        while (!Thread.currentThread().isInterrupted()) {
            var consumer = fromRadioHandler.get();
            if (consumer != null) {
                try {
                    var data = pool.poll(options.smallDelay(), TimeUnit.MILLISECONDS);
                    if (data != null) {
                        consumer.accept(data);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    LOGGER.error("handing from radio", e);
                }
            } else {
                try {
                    LOGGER.warn("no FromRadio handler");
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void handleDelivery(byte[] dst) {
        try {
            var rx = FromRadio.parseFrom(dst);
            if (rx.getPayloadVariantCase() == PayloadVariantCase.QUEUESTATUS) {
                queueFree = rx.getQueueStatus().getFree();
                LOGGER.debug("queue status: {}", rx.getQueueStatus());
            } else {
                try {
                    pool.put(rx);
                } catch (InterruptedException e) {
                    LOGGER.warn("while adding to the pool", e);
                    Thread.currentThread().interrupt();
                }
            }
        } catch (InvalidProtocolBufferException e) {
            LOGGER.error("{} on data: {}", e.getMessage(), dst);
        }
    }


    private byte[] radioWakeup() {
        byte[] wakeup = new byte[4];
        for (int n = 0; n < 4; n++) {
            wakeup[n] = (byte) START1;
        }
        return wakeup;
    }

    private class FromRadioReader {

        int ptr;
        int packetLen;
        int msb, lsb;
        boolean hasPacket;

        void deliverPacket() {
            if (packetLen > 0) {

                byte[] dst = new byte[packetLen];
                rxPacket.get(dst, 0, packetLen);

                handleDelivery(dst);
            }
            rxPacket.reset();
            reset();
            hasPacket = false;

            if (proxy != null) {
                proxy.rxFlush();
            }
        }


        int reset() {
            ptr = 0;
            packetLen = 0;
            return 0;
        }


        void readChar(int c) {
            // Assume we will be advancing our pointer
            var nextPtr = ptr + 1;

            switch (ptr) {
                case 0 -> {// looking for START1
                    if (c != START1) {
                        LOGGER.debug("first char {}", c);
                        nextPtr = reset();
                    }
                }
                case 1 -> {
                    // Looking for START2
                    if (c != START2) {
                        LOGGER.error("Lost protocol sync");
                        nextPtr = 0;
                    }
                }

                case 2 -> { // Looking for MSB of our 16 bit length
                    msb = c & 0xff;
                }
                case 3 -> { // Looking for LSB of our 16 bit length
                    lsb = c & 0xff;

                    // We've read our header, do one big read for the packet itself
                    packetLen = (msb << 8) | lsb;

                    LOGGER.debug("packet len: {}, MSB {}, LSB {}", packetLen, msb, lsb);

                    if (packetLen > MAX_TO_FROM_RADIO_SIZE) {
                        nextPtr = 0;
                    } else if (packetLen == 0) {
                        hasPacket = true; // zero length packets are valid and should be delivered immediately (because there won't be a next byte of payload)
                    }
                }

                default -> {
                    byte d = (byte) c;
                    // We are looking at the packet bytes now
                    rxPacket.put(ptr - HEADER_LEN, d);

                    // Note: we have to check if ptr +1 is equal to packet length (for example, for a 1 byte packetlen, this code will be run with ptr of4
                    if (ptr - HEADER_LEN + 1 == packetLen) {
                        hasPacket = true;
                    }
                }
            }
            if (hasPacket) {
                deliverPacket();
            } else {
                ptr = nextPtr;
            }
        }
    }

    public void stop() {
        sendToRadio(ToRadio.newBuilder().setDisconnect(true).build().toByteArray());
        nap();
        tg.interrupt();
    }

    public boolean isRunning() {
        return tg.activeCount() == threads.length;
    }
}
