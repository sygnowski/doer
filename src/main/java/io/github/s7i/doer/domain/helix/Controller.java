package io.github.s7i.doer.domain.helix;

import io.github.s7i.doer.DoerException;
import org.apache.helix.controller.HelixControllerMain;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static java.util.Objects.requireNonNull;

public class Controller extends HelixMember {

    static class InstanceHolder {
        private static Controller instance;

        public static Controller getInstance() {
            return requireNonNull(instance, "controller instance");
        }
    }

    CountDownLatch countDownLatch = new CountDownLatch(1);

    public Controller(String instanceName, String clusterName, String server) throws Exception {
        super(instanceName, clusterName, server);

        synchronized (InstanceHolder.class) {
            if (InstanceHolder.instance != null) {
                throw new DoerException("illegal state");
            }
            InstanceHolder.instance = this;
        }

        helixManager = HelixControllerMain.startHelixController(server, clusterName, instanceName,
                HelixControllerMain.STANDALONE);

        countDownLatch.await();
    }

    @Override
    public void cleanup() {
        super.cleanup();

        countDownLatch.countDown();
    }

    public Map<String, String> onClusterRebalance(Map<String, String> instanceMapping) {
        return instanceMapping;
    }
}
