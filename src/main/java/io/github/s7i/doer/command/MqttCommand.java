package io.github.s7i.doer.command;

import io.github.s7i.doer.DoerException;
import io.github.s7i.doer.domain.mqtt.MqttClientVerticle;
import io.vertx.core.Vertx;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

@CommandLine.Command(
        name = "mqtt",
        description = "MQTT Client"
)
@Slf4j
public class MqttCommand extends Command{

    @CommandLine.Option(names = "--port", defaultValue = "1883")
    Integer port;
    @CommandLine.Option(names = "--server", required = true)
    String serverAddress;
    @CommandLine.Option(names = "--topic", required = true)
    String topic;
    @CommandLine.Option(names = "--clientId")
    String clientId;
    @CommandLine.Option(names = "--user", defaultValue = "")
    String user;
    @CommandLine.Option(names = "--password", defaultValue = "")
    String password;
    @CommandLine.Option(names = "--qos", defaultValue = "0")
    Integer qos;


    private MqttClientVerticle createMqttVerticle() {
        return new MqttClientVerticle()
              .serverAddress(serverAddress)
              .port(port)
              .topic(topic)
              .user(user)
              .password(password)
              .clientId(clientId == null ? "doer" + UUID.randomUUID() : clientId)
              .qos(qos);
    }

    @Override
    public void onExecuteCommand() {
        try {
            var vertex = Vertx.vertx();
            var latch = new CountDownLatch(1);
            Runnable cleanup = () -> {
                log.info("running shutdown");
                vertex.close().onSuccess(e -> log.info("close success: {}", e));
                latch.countDown();
            };
            Runtime.getRuntime().addShutdownHook(new Thread(cleanup));

            vertex.deployVerticle(createMqttVerticle(), ar -> {
                if (ar.succeeded()) {
                    log.info("deployed");
                } else {
                    log.error("oops", ar.cause());
                }
            });
            latch.await();
            log.info("end");
        } catch (Exception e) {
            throw new DoerException(e);
        }
    }
}
