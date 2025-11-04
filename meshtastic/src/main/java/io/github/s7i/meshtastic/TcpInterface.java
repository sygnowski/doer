package io.github.s7i.meshtastic;

import com.geeksville.mesh.MeshProtos.FromRadio;
import com.geeksville.mesh.MeshProtos.ToRadio;
import com.google.protobuf.InvalidProtocolBufferException;
import io.github.s7i.meshtastic.proxy.StreamProxy;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpInterface implements MeshtasticInterface {

    public static final Logger LOGGER = LoggerFactory.getLogger(TcpInterface.class);

    private final int port;
    private final String host;
    private final StreamProxy proxy;
    private Socket socket;
    private MeshtasticStream stream;
    private InputStream is;
    private OutputStream os;
    private volatile Consumer<byte[]> handler;
    private volatile Runnable onStop;

    public TcpInterface(int port, String host) {
        this(port, host, null);
    }

    public TcpInterface(int port, String host, StreamProxy proxy) {
        this.port = port;
        this.host = host;
        this.proxy = proxy;
    }

    @Override
    public void connect() {
        try {
            var inet = InetAddress.getByName(host);

            socket = new Socket(inet, port);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(500);

            LOGGER.debug("is connected {}", socket.isConnected());

            is = new BufferedInputStream(socket.getInputStream());
            os = new BufferedOutputStream(socket.getOutputStream());

            stream = new MeshtasticStream(is, os);
            stream.setProxy(proxy);
            stream.setHandler(this::onRx);
            stream.onStop(this::onStop);
            stream.startReadFromRadio(true);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void disconnect() {
        try {
            stream.stop();
            TimeUnit.MILLISECONDS.sleep(stream.getOptions().smallDelay() + 10);
            is.close();
            os.close();
            socket.close();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void sendToRadio(byte[] toRadio) {
        try {
            var rx = ToRadio.parseFrom(toRadio);
            LOGGER.debug("RX {}", rx);
            stream.send(rx);
        } catch (InvalidProtocolBufferException e) {
            LOGGER.error("invalid", e);
        }

    }

    private void onRx(FromRadio fromRadio) {
        if (handler != null) {
            handler.accept(fromRadio.toByteArray());
        }
    }

    @Override
    public void handleFromRadio(Consumer<byte[]> fromRadio) {
        handler = fromRadio;
    }

    public void setOnStop(Runnable onStop) {
        this.onStop = onStop;
    }

    private void onStop() {
        if (onStop != null) {
            onStop.run();
        }
    }
}
