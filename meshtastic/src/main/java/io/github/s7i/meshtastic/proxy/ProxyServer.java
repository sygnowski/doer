package io.github.s7i.meshtastic.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyServer {

    public static final Logger LOGGER = LoggerFactory.getLogger(ProxyServer.class);
    public static final int BUFFER_SMALL = Integer.getInteger("buffer.small", 1024);

    public ProxyServer(String host, int port) {
        this.host = host;
        this.port = port;
        looper = new Thread(this::startServer, "Proxy Event Loop");
        looper.setDaemon(true);
    }

    private final String host;
    private final int port;
    private final Thread looper;
    private BufferProxy proxy;

    public void start() {
        looper.start();
    }

    public StreamProxy proxy() {
        proxy = new BufferProxy();
        return proxy;
    }

    private void startServer() {
        try (ServerSocketChannel server = ServerSocketChannel.open()) {

            server.bind(new InetSocketAddress(host, port));
            server.configureBlocking(false);

            Selector selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);

            while (!Thread.currentThread().isInterrupted()) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();

                    if (key.isAcceptable()) {
                        SocketChannel client = server.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

                        accept(client);

                        //key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    } else if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();

                        read(client);

                        //key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    } else if (key.isWritable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        write(client);

                        //key.interestOps(SelectionKey.OP_READ);

                    }
                    keys.remove();
                }
            }
        } catch (Exception e) {
            LOGGER.error("oops", e);
        }
    }

    private void read(SocketChannel client) {

        proxy.doRx(rx -> {
            var buff = ByteBuffer.allocate(BUFFER_SMALL);
            try {
                client.read(buff);
                buff.flip();

                rx.put(buff);

                if (buff.hasRemaining()) {
                    LOGGER.warn("rx has remaining");
                }
            } catch (IOException e) {
                LOGGER.error("handle rx", e);
            }
        });

    }

    private void write(SocketChannel client) {

        proxy.doTx(tx -> {
            try {
                client.write(tx);

            } catch (Exception e) {
                LOGGER.warn("while write", e);
            }
        });

    }

    private void accept(SocketChannel client) {
        LOGGER.info("new client {}", client);
    }
}
