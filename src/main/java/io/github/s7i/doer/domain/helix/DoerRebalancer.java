package io.github.s7i.doer.domain.helix;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.helix.controller.rebalancer.DelayedAutoRebalancer;
import org.apache.helix.controller.rebalancer.constraint.MonitoredAbnormalResolver;
import org.apache.helix.controller.stages.CurrentStateOutput;
import org.apache.helix.model.ClusterConfig;
import org.apache.helix.model.IdealState;
import org.apache.helix.model.Partition;
import org.apache.helix.model.StateModelDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j(topic = "doer.console")
public class DoerRebalancer extends DelayedAutoRebalancer {


    @Data
    @Accessors(fluent = true)
    public static class Context {
        @JsonProperty
        StateModelDefinition model;
        @JsonProperty
        IdealState idealState;
    }

    @Override
    protected Map<String, String> computeBestPossibleStateForPartition(Set<String> liveInstances, StateModelDefinition stateModelDef, List<String> preferenceList, CurrentStateOutput currentStateOutput, Set<String> disabledInstancesForPartition, IdealState idealState, ClusterConfig clusterConfig, Partition partition, MonitoredAbnormalResolver monitoredResolver) {
        var mapping = super.computeBestPossibleStateForPartition(liveInstances, stateModelDef, preferenceList, currentStateOutput, disabledInstancesForPartition, idealState, clusterConfig, partition, monitoredResolver);

        synchronized (Controller.InstanceHolder.class) {
            mapping = Controller.InstanceHolder.getInstance().onClusterRebalance(
                    mapping,
                    new Context()
                            .model(stateModelDef)
                            .idealState(idealState)
            );
        }

        log.info("[HELIX|DoerRebalancer] Final Mapping: {}", mapping);
        return mapping;
    }
}
