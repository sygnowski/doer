package io.github.s7i.meshtastic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class StreamProxyTest {


    @Test
    void testRxAndFlush() throws IOException {

        var prox = new StreamProxy();
        var tx = new ByteArrayOutputStream(20);
        prox.os = tx;

        prox.rx(10);
        prox.rx(11);
        prox.rx(12);
        prox.rx(13);
        prox.rx(14);

        prox.rxFlush();

        assertEquals(5, tx.size());

        prox.rx(20);
        prox.rx(21);
        prox.rx(22);
        prox.rx(23);
        prox.rx(24);

        prox.rxFlush();

        var result = tx.toByteArray();
        assertArrayEquals(new byte[]{
              10, 11, 12, 13, 14, 20, 21, 22, 23, 24
        }, result);


    }

}