package io.github.s7i.doer.domain.helix;

import org.apache.helix.controller.HelixControllerMain;

import java.util.concurrent.CountDownLatch;

public class Controller extends HelixMember {

    CountDownLatch countDownLatch = new CountDownLatch(1);
    public Controller(String instanceName, String clusterName, String server) throws Exception {
        super(instanceName, clusterName, server);

        helixManager = HelixControllerMain.startHelixController(server, clusterName, instanceName,
                HelixControllerMain.STANDALONE);

        countDownLatch.await();
    }

    @Override
    public void cleanup() {
        super.cleanup();

        countDownLatch.countDown();
    }
}
