package io.github.s7i.doer.domain.mqtt;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.UUID;
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;

@CommandLine.Command(
        name = "mqtt",
        description = "MQTT Client"
)
@Slf4j(topic = "doer.console")
public class MqttCommand implements Callable<Integer> {

    @CommandLine.Option(names = "--port", defaultValue = "1883")
    Integer port;
    @CommandLine.Option(names = "--server", required = true)
    String serverAddress;
    @CommandLine.Option(names = "--topic", required = true)
    String topic;
    @CommandLine.Option(names = "--clientId")
    String clientId;
    @CommandLine.Option(names = "--user", required = true)
    String user;
    @CommandLine.Option(names = "--password", required = true)
    String password;


    public static final int QOS = 2;

    @NoArgsConstructor
    @Setter
    @Accessors(fluent = true)
    static class MqttClientVerticle extends AbstractVerticle {
        int port;
        String serverAddress;
        String topic;
        String user;
        String password;
        String clientId;
        private MqttClient client;

        @Override
        public void init(Vertx vertx, Context context) {

            var opt = new MqttClientOptions();
            opt.setClientId(requireNonNull(clientId));
            opt.setUsername(requireNonNull(user));
            opt.setPassword(requireNonNull(password));

            client = MqttClient.create(vertx, opt);
        }

        @Override
        public void start(Promise<Void> startPromise) throws Exception {
            client.connect(port, serverAddress).onComplete(done -> {
                client.publishHandler(s -> {
                            log.info("There are new message in topic: {}", s.topicName());
                            log.info("Content(as string) of the message: {}", s.payload().toString());
                            log.info("QoS: {}", s.qosLevel());
                        })
                        .subscribe(topic, QOS);
            });
        }

        @Override
        public void stop(Promise<Void> stopPromise) throws Exception {
            client.disconnect();
        }
    }


    @Override
    public Integer call() throws Exception {


        var vertex = Vertx.vertx();
        var verticle = new MqttClientVerticle()
                .serverAddress(serverAddress)
                .port(port)
                .topic(topic)
                .user(user)
                .password(password)
                .clientId(clientId == null ? "doer" + UUID.randomUUID() : clientId);
        vertex.deployVerticle(verticle);

        Runnable cleanup = () -> {
            log.info("running shutdown");
            vertex.close().onSuccess(e -> log.info("close success: {}", e));
        };
        Runtime.getRuntime().addShutdownHook(new Thread(cleanup));


        return 0;
    }
}
