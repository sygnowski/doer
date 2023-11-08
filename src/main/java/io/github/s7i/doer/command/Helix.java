package io.github.s7i.doer.command;

import io.github.s7i.doer.Doer;
import io.github.s7i.doer.domain.helix.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.helix.model.IdealState;
import org.apache.helix.model.MasterSlaveSMD;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

@Command(name = "helix", description = "Helix Toolkit.", showDefaultValues = true, subcommands = MessageCmd.class)
@Slf4j(topic = "doer.console")
public class Helix implements Callable<Integer> {

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

    @Option(names = "--stateModel", defaultValue = MasterSlaveSMD.name)
    String stateModel;

    @Option(names = "--rebalanceMode", defaultValue = "FULL_AUTO", description = "Other options: ${COMPLETION-CANDIDATES}")
    IdealState.RebalanceMode rebalanceMode;

    @Option(names = "--num-partition", defaultValue = "1")
    Integer numPartitions;

    @Option(names = "--replicas", defaultValue = "1")
    Integer replicas;

    @Option(names = "--model", description = "Helix State Model YAML definition file.")
    Path helixModel;

    @Option(names = "--help", usageHelp = true)
    boolean help;

    @Option(names = "-f")
    Map<String, String> flags = Collections.emptyMap();

    @SneakyThrows
    @Override
    public Integer call() {
        if (nonNull(helixModel)) {
            return admin().setupCluster(helixModel);
        }

        requireNonNull(clusterName, "missing: cluster name");
        requireNonNull(type, "type");
        switch (type) {
            case "uis":
                log.info("update ideal state");
                return admin().simpleFields(simpleFields).updateIdealState();
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
            case "create":
                return admin().setupCluster();
            case "delete":
                return admin().deleteCluster();
            case "add":
                return admin().addInstance();
            case "rebalance":
                return admin().rebalance();
        }
        return Doer.EC_INVALID_USAGE;
    }

    private void runController() throws Exception {
        new Controller(instanceName, clusterName, server).flags(flags).enable();
    }

    private void runParticipant() throws Exception {
        new Participant(instanceName, clusterName, server).flags(flags).enable();
    }

    private void runSpectator() throws Exception {
        new Spectator(instanceName, clusterName, server).flags(flags).enable();
    }

    private Admin admin() {
        return new Admin()
                .server(server)
                .clusterName(clusterName)
                .instanceName(instanceName)
                .resource(resource)
                .stateModel(stateModel)
                .rebalanceMode(rebalanceMode)
                .numPartitions(numPartitions)
                .replicas(replicas);
    }

}
