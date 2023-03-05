package io.github.s7i.doer.domain.helix;

import io.github.s7i.doer.Doer;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.helix.manager.zk.ZKHelixManager;
import org.apache.helix.model.HelixConfigScope;
import org.apache.helix.model.IdealState;
import org.apache.helix.model.builder.HelixConfigScopeBuilder;
import org.apache.helix.tools.ClusterSetup;
import org.apache.helix.tools.commandtools.YAMLClusterSetup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Setter
@Accessors(fluent = true, chain = true)
@NoArgsConstructor
@ToString
@Slf4j(topic = "doer.console")
public class Admin {

    String server;
    String clusterName;
    String instanceName;
    String resource;
    Map<String, String> simpleFields;
    String stateModel;
    IdealState.RebalanceMode rebalanceMode;
    Integer numPartitions;
    Integer replicas;

    boolean allowParticipantAutoJoin = true;

    public Integer setupCluster(Path model) {
        log.info("loading cluster definition from file: {}", model);

        if (!Files.exists(model)) {
            log.info("File don't exists: {}", model.toAbsolutePath());
            return Doer.EC_ERROR;
        }

        try (var is = Files.newInputStream(model)) {
            new YAMLClusterSetup(server).setupCluster(is);
        } catch (IOException e) {
            log.error("oops", e);
            return Doer.EC_ERROR;
        }
        return 0;
    }

    public Integer setupCluster() {

        log.info("setup cluster: {}", this);

        check();

        requireNonNull(resource, "missing resource name");
        requireNonNull(replicas, "missing replicas");
        requireNonNull(rebalanceMode, "missing rebalance mode");
        requireNonNull(numPartitions, "missing num partitions");

        var cs = new ClusterSetup.Builder()
                .setZkAddress(server)
                .build();
        cs.addCluster(clusterName, false);
        cs.addResourceToCluster(clusterName, resource, numPartitions, stateModel, rebalanceMode.name());
        var admin = cs.getClusterManagementTool();

        admin.enableResource(clusterName, resource, true);
        admin.enableCluster(clusterName, true);

        admin.rebalance(clusterName, resource, replicas);


        var scope = new HelixConfigScopeBuilder(HelixConfigScope.ConfigScopeProperty.CLUSTER)
                .forCluster(clusterName)
                .build();

        admin.setConfig(scope, Map.of(
                ZKHelixManager.ALLOW_PARTICIPANT_AUTO_JOIN,
                String.valueOf(allowParticipantAutoJoin))
        );

        cs.close();

        return 0;
    }

    private void check() {
        requireNonNull(server, "missing server address");
        requireNonNull(clusterName, "missing cluster name");
    }

    public Integer addInstance() {
        var cs = new ClusterSetup.Builder()
                .setZkAddress(server)
                .build();

        cs.addInstanceToCluster(clusterName, instanceName);

        log.info("instance added: {}", instanceName);

        cs.close();

        return 0;
    }

    public Integer rebalance() {
        check();

        requireNonNull(resource);
        requireNonNull(replicas);


        var cs = new ClusterSetup.Builder()
                .setZkAddress(server)
                .build();

        cs.rebalanceCluster(clusterName, resource, replicas, "", "");

        log.info("rebalancing...");

        return 0;
    }

    public Integer deleteCluster() {
        check();

        var cs = new ClusterSetup.Builder()
                .setZkAddress(server)
                .build();

        cs.deleteCluster(clusterName);

        cs.close();

        return 0;
    }

    @SneakyThrows
    public Integer updateIdealState() {
        check();

        var updater = new IdealStateUpdater(instanceName, clusterName, server);
        updater.setResource(resource);
        updater.setSimpleFields(simpleFields);
        updater.enable();

        return 0;
    }


}
