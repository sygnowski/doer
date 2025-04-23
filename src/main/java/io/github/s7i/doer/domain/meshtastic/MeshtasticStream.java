package io.github.s7i.doer.domain.meshtastic;

import io.github.s7i.meshtastic.Proto;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeshtasticStream {

    public static final Logger LOGGER = LoggerFactory.getLogger(MeshtasticStream.class);

    public static final int START1 = 0x94;
    public static final int START2 = 0xC3;
    public static final int HEADER_LEN = 4;
    public static final int NODELESS_WANT_CONFIG_ID = 69420;
    public static final int MAX_TO_FROM_RADIO_SIZE = 512;
    private static final int TOO_MANY_ERROR = 100;


    private final InputStream is;
    private final OutputStream os;

    private final ByteBuffer rxPacket = ByteBuffer.allocate(MAX_TO_FROM_RADIO_SIZE).mark();
    private final ThreadGroup tg;
    private final ArrayBlockingQueue<byte[]> pool;
    private final Thread[] threads;

    public MeshtasticStream(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;

        pool = new ArrayBlockingQueue<>(100);
        tg = new ThreadGroup("Meshtastic Radio");

        threads = new Thread[]{
              new Thread(tg, this::handleRadioRx, "FromRadio"),
              new Thread(tg, this::handleHeartBeat, "HeartBeat")
        };
    }

    public ArrayBlockingQueue<byte[]> getPool() {
        return pool;
    }

    public void sendToRadio(byte[] data) {
        var len = data.length;

        var header = ByteBuffer.allocate(HEADER_LEN)
              .put((byte) START1)
              .put((byte) START2)
              .put((byte) ((len >> 8) & 0xFF))
              .put((byte) (len & 0xFF));

        try {
            os.write(header.array());
            os.write(data);
            os.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void startReadFromRadio(boolean withNodes) throws Exception {
        os.write(radioWakeup());
        os.flush();

        TimeUnit.MILLISECONDS.sleep(100);
        var configId = withNodes ? new Random().nextInt() : NODELESS_WANT_CONFIG_ID;

        sendToRadio(Proto.INSTANCE.getConfiguration(configId).toByteArray());

        for (var thr : threads) {
            thr.setDaemon(true);
            thr.start();
        }
    }

    void handleRadioRx() {
        FromRadioReader reader = new FromRadioReader();
        LOGGER.debug("starting rx");
        int errorCount = 0;
        while (errorCount < TOO_MANY_ERROR && !Thread.currentThread().isInterrupted()) {
            try {
                int c = is.read();

                errorCount = 0;

                if (c != -1) {
                    reader.readChar(c);
                } else {
                    LOGGER.warn("TCP EOF");
                    break;
                }
            } catch (SocketTimeoutException e) {
                errorCount++;

                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        LOGGER.debug("stopping rx");
    }

    void handleHeartBeat() {
        while (!Thread.currentThread().isInterrupted()) {
            sendToRadio(Proto.INSTANCE.heartbea().toByteArray());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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

        // Deliver our current packet and restart our reader
        void deliverPacket() {
//            val buf = rxPacket.copyOf(packetLen)
//            service.handleFromRadio(buf)

            if (packetLen > 0) {

                byte[] dst = new byte[packetLen];
                rxPacket.get(dst, 0, packetLen);

                try {
                    pool.put(dst);
                } catch (InterruptedException e) {
                    LOGGER.warn("while adding to the pool", e);
                    Thread.currentThread().interrupt();
                }
            }
            rxPacket.reset();
            reset();
            hasPacket = false;
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
        tg.interrupt();
    }

    public boolean isRunning() {
        return tg.activeCount() == threads.length;
    }
}
