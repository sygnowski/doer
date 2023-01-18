package io.github.s7i.doer.command;

import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.domain.helix.Controller;
import io.github.s7i.doer.domain.helix.IdealStateUpdater;
import io.github.s7i.doer.domain.helix.Participant;
import io.github.s7i.doer.domain.helix.Spectator;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "helix", description = "Runs the Helix Spectator")
@Slf4j(topic = "doer.console")
public class Helix implements Runnable {

    @Option(names = "-s", defaultValue = "localhost:2181")
    String server;

    @Option(names = "-c")
    String clusterName;

    @Option(names = "-n", defaultValue = "doer")
    String instanceName;

    @Option(names = "-t", defaultValue = "spectator")
    String type;

    @Option(names = "-r")
    String resource;

    @Option(names = "-sf")
    Map<String, String> simpleFields;

    @SneakyThrows
    @Override
    public void run() {
        requireNonNull(type, "type");
        switch (type) {
            case "isu":
                log.info("update ideal state");
                updateIdealState();
                break;
            case "s":
            case "spectator":
                log.info("running a helix spectator...");
                runSpectator();
                break;
            case "p":
            case "participant":
                log.info("running a helix participant...");
                runParticipant();
                break;
            case "c":
            case "controller":
                log.info("running a helix controller...");
                runController();
                break;
        }
    }

    private void updateIdealState() {
        var updater = new IdealStateUpdater(instanceName, clusterName, server);
        updater.setResource(resource);
        updater.setSimpleFields(simpleFields);
        updater.update();
    }

    private void runController() throws Exception {
        new Controller(instanceName, clusterName, server);
    }

    private void runParticipant() throws Exception {
        new Participant(instanceName, clusterName, server);
    }

    private void runSpectator() throws Exception {
        new Spectator(instanceName, clusterName, server);
    }


}
