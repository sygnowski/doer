package io.github.s7i.meshtastic.proxy;

import static io.github.s7i.meshtastic.proxy.BufferConfig.BUFFER_SMALL;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyServer {

    public static final Logger LOGGER = LoggerFactory.getLogger(ProxyServer.class);

    private final String host;
    private final int port;
    private final Thread[] pool;
    private final ThreadGroup proxyGroup = new ThreadGroup("Proxy Server");
    private BufferProxy proxy;

    private Selector selector;
    private final Queue<Runnable> pendingChanges = new ConcurrentLinkedQueue<>();
    private final Map<String, SocketChannel> clientsByAddress = new ConcurrentHashMap<>();
    private final Map<SocketChannel, Queue<ByteBuffer>> pendingWrites = new ConcurrentHashMap<>();

    public ProxyServer(String host, int port) {
        this.host = host;
        this.port = port;

        var eventLoop = new Thread(proxyGroup, this::startServer, "Proxy Event Loop");
        eventLoop.setDaemon(true);

        var rxBufferObserver = new Thread(proxyGroup, this::observForRxToClients, "Client RX Observer");
        rxBufferObserver.setDaemon(true);

        pool = new Thread[]{
              eventLoop, rxBufferObserver
        };
    }

    public void start() {
        if (proxy == null) {
            throw new IllegalStateException("call ProxyServer::proxy() before");
        }
        for (var thr : pool) {
            thr.start();
        }
    }

    public BufferProxy proxy() {
        proxy = new BufferProxy();
        return proxy;
    }

    private void observForRxToClients() {
        while (!Thread.currentThread().isInterrupted()) {
            if (clientsByAddress.isEmpty()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                proxy.waitForDataForTx();
                proxy.doTx(this::sendToAllClients);
            }
        }
    }

    private void sendToAllClients(ByteBuffer tx) {
        if (!tx.hasRemaining()) {
            return;
        }
        byte[] sharedBuffer = new byte[tx.remaining()];
        tx.get(sharedBuffer);

        for (var channel : clientsByAddress.values()) {
            var toClient = ByteBuffer.allocate(sharedBuffer.length);
            toClient.put(sharedBuffer);
            toClient.flip();

            send(channel, toClient);
        }
    }


    public void startServer() {
        try {

            selector = Selector.open();
            var serverChannel = ServerSocketChannel.open();

            serverChannel.bind(new InetSocketAddress(host, port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (!Thread.currentThread().isInterrupted()) {
                selector.select();

                Runnable change;
                while ((change = pendingChanges.poll()) != null) {
                    change.run();
                }

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    try {
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        } else if (key.isWritable()) {
                            handleWrite(key);
                        }
                    } catch (IOException e) {
                        key.cancel();
                        key.channel().close();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("server fatal error", e);
        }
    }


    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);

        String addr = client.getRemoteAddress().toString();
        clientsByAddress.put(addr, client);
        pendingWrites.put(client, new ConcurrentLinkedQueue<>());

        LOGGER.info("new client: {}", addr);

    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SMALL);
        int read = client.read(buffer);

        if (read == -1) {
            String addr = client.getRemoteAddress().toString();
            LOGGER.info("Disconnected: {}", addr);
            clientsByAddress.remove(addr);
            pendingWrites.remove(client);
            client.close();
            key.cancel();
            return;
        }

        buffer.flip();
        proxy.doRx(rx -> rx.put(buffer));
    }

    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        Queue<ByteBuffer> queue = pendingWrites.get(client);

        if (queue == null) {
            return;
        }

        while (!queue.isEmpty()) {
            ByteBuffer buffer = queue.peek();
            LOGGER.debug("client rx: {}", buffer.remaining());

            client.write(buffer);
            if (buffer.hasRemaining()) {
                break;
            }

            queue.poll();
        }

        if (queue.isEmpty()) {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    public void send(SocketChannel client, ByteBuffer data) {
        pendingChanges.add(() -> {
            SelectionKey key = client.keyFor(selector);
            if (key == null || !key.isValid()) {
                return;
            }

            Queue<ByteBuffer> queue = pendingWrites.get(client);
            if (queue != null) {
                queue.add(data);
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }
        });
        selector.wakeup();
    }
}
