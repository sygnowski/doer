package io.github.s7i.doer.domain.helix;

import io.github.s7i.doer.Doer;
import io.github.s7i.doer.DoerException;
import lombok.extern.slf4j.Slf4j;
import org.apache.helix.HelixManager;
import org.apache.helix.InstanceType;
import org.apache.helix.zookeeper.datamodel.ZNRecord;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Participant extends HelixMember {

    CountDownLatch countDownLatch = new CountDownLatch(1);

    public Participant(String instanceName, String clusterName, String server) {
        super(instanceName, clusterName, server);
    }

    @Override
    public void enable() throws Exception {
        super.enable();

        connect(InstanceType.PARTICIPANT);

        countDownLatch.await();
    }

    @Override
    public void cleanup() {
        super.cleanup();
        countDownLatch.countDown();
    }

    @Override
    protected void onBefore(HelixManager manager) {
        var engine = manager.getStateMachineEngine();

        if (!engine.registerStateModelFactory(MasterSlaveModel.MODEL, new MasterSlaveModel.Factory(this))) {
            throw new DoerException("model not registered");
        }

        if (!engine.registerStateModelFactory(GradeStateModel.MODEL, new GradeStateModel.Factory(this))) {
            throw new DoerException("model not registered");
        }


        var record = new ZNRecord(UUID.randomUUID().toString());
        record.setSimpleField("X_TEST", "test of simple field");

        manager.setLiveInstanceInfoProvider(() -> record);
    }

    @Override
    protected void onAfter(HelixManager manager) {

        CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS)
                .execute(this::digGold);

    }

    private void digGold() {
        final var startLevel = Long.parseLong(flags.getOrDefault("gold.start", "0"));
        final var incVal = Long.parseLong(flags.getOrDefault("gold.inc", "10"));
        final var sleep = Long.parseLong(flags.getOrDefault("gold.sleep", "10"));

        long goldLevel = startLevel;

        while (!Thread.currentThread().isInterrupted()) {

            goldLevel += incVal;

            updateResource("gold", Long.toString(goldLevel));
            Doer.console().info("{} gold level: {}", instanceName, goldLevel);
            try {
                TimeUnit.SECONDS.sleep(sleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("gold task end");
    }

}
