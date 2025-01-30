package io.github.s7i.doer.domain.mqtt;

import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.ConsoleLog;
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

@NoArgsConstructor
@Setter
@Accessors(fluent = true)
@Slf4j
public class MqttClientVerticle extends AbstractVerticle implements ConsoleLog {

    private int port;
    private String serverAddress;
    private String topic;
    private String user;
    private String password;
    private String clientId;
    private MqttClient client;
    private int qos;

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
        client.connect(port, serverAddress)
              .onFailure(thr -> log.error("oops", thr))
              .onComplete(done -> {
                  subscribe();
                  startPromise.complete();
                  log.info("MQTT Client running...");
              });
    }

    private void subscribe() {
        client.publishHandler(s -> {
                  info("There are new message in topic: {}", s.topicName());
                  info("Content(as string) of the message: {}", s.payload().toString());
                  info("QoS: {}", s.qosLevel());
              })
              .subscribe(topic, qos);
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        client.disconnect();
    }
}
