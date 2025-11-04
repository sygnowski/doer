package io.github.s7i.meshtastic.proxy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class StreamProxyTest {

    @Test
    void testRxAndFlush() throws IOException {


        var buff = ByteBuffer.allocate(10);
        var prox = new BufferProxy();

        prox.rx(10);
        prox.rx(11);
        prox.rx(12);
        prox.rx(13);
        prox.rx(14);

        prox.rxFlush();


        prox.rx(20);
        prox.rx(21);
        prox.rx(22);
        prox.rx(23);
        prox.rx(24);

        prox.rxFlush();

        prox.doTx(buff::put);

        var result = buff.array();
        assertArrayEquals(new byte[]{
              10, 11, 12, 13, 14, 20, 21, 22, 23, 24
        }, result);


    }

}