package io.github.s7i.doer.command;

import io.github.s7i.doer.domain.mqtt.MqttClientVerticle;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

@CommandLine.Command(
        name = "mqtt",
        description = "MQTT Client"
)
@Slf4j
public class MqttCommand extends VerticleCommand {

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


    protected MqttClientVerticle createVerticle() {
        return new MqttClientVerticle()
              .serverAddress(serverAddress)
              .port(port)
              .topic(topic)
              .user(user)
              .password(password)
              .clientId(clientId == null ? "doer" + UUID.randomUUID() : clientId)
              .qos(qos);
    }

}
