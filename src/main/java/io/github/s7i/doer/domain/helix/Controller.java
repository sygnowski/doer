package io.github.s7i.doer.domain.helix;

import io.github.s7i.doer.DoerException;
import lombok.extern.slf4j.Slf4j;
import org.apache.helix.controller.HelixControllerMain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static java.util.Objects.requireNonNull;

@Slf4j(topic = "doer.console")
public class Controller extends HelixMember {

    static class InstanceHolder {
        private static Controller instance;

        public static Controller getInstance() {
            return requireNonNull(instance, "controller instance");
        }
    }

    CountDownLatch countDownLatch = new CountDownLatch(1);

    public Controller(String instanceName, String clusterName, String server) {
        super(instanceName, clusterName, server);
    }

    @Override
    public void enable() throws Exception {
        synchronized (InstanceHolder.class) {
            if (InstanceHolder.instance != null) {
                throw new DoerException("illegal state");
            }
            InstanceHolder.instance = this;
        }

        helixManager = HelixControllerMain.startHelixController(server, clusterName, instanceName,
                HelixControllerMain.STANDALONE);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();

        countDownLatch.countDown();
    }

    /**
     * @param instanceMapping key: instanceName
     *                        value: state
     * @return mapping
     */
    public Map<String, String> onClusterRebalance(Map<String, String> instanceMapping, DoerRebalancer.Context context) {
        var remapped = new HashMap<>(instanceMapping);

        instanceMapping.keySet()
                .stream()
                .map(this::getLiveInstance)
                .flatMap(Optional::stream)
                .forEach(liveInstance -> log.info("[{}] resources: {}", liveInstance.getInstanceName(), liveInstance.getResourceCapacityMap()));

        if (instanceMapping.entrySet().stream().noneMatch(e -> e.getValue().equals(GradeStateModel.ALPHA))) {


            var goldMin = Long.parseLong(flags().getOrDefault("gold.min", "50"));
            var goldScore = new GradeStateModel.GoldScore(goldMin);

            instanceMapping.keySet()
                    .stream()
                    .map(this::getLiveInstance)
                    .flatMap(Optional::stream)
                    .map(GradeStateModel.GoldInfo::new)
                    .filter(GradeStateModel.GoldInfo::hasGoldLevel)
                    .forEach(goldScore::offer);


            goldScore.getAlpha().ifPresent(alpha -> {
                remapped.put(alpha.getInstance().getInstanceName(), GradeStateModel.ALPHA);
            });

            goldScore.getBeta(2).forEach(beta -> {
                remapped.put(beta.getInstance().getInstanceName(), GradeStateModel.BETA);
            });

        }

        getEventLogger().onRebalance(Map.of(
                "context", context,
                "before", instanceMapping,
                "after", remapped
        ));

        return remapped;
    }
}
