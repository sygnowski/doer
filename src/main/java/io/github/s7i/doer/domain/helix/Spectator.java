package io.github.s7i.doer.domain.helix;

import lombok.extern.slf4j.Slf4j;
import org.apache.helix.InstanceType;

import java.util.concurrent.CountDownLatch;

import static io.github.s7i.doer.domain.helix.Utll.LISTENERS;
import static io.github.s7i.doer.domain.helix.Utll.LISTENERS_ALL;

@Slf4j(topic = "doer.console")
public class Spectator extends HelixMember {


    CountDownLatch countDownLatch = new CountDownLatch(1);

    public Spectator(String instanceName, String clusterName, String server) throws Exception {
        super(instanceName, clusterName, server);
    }

    @Override
    public void enable() throws Exception {
        connect(InstanceType.SPECTATOR);
        performEnableListeners(flags.getOrDefault(LISTENERS, LISTENERS_ALL));

        countDownLatch.await();
        log.info("Spectator ends.");
    }

    @Override
    public void cleanup() {
        countDownLatch.countDown();
        super.cleanup();

    }
}
