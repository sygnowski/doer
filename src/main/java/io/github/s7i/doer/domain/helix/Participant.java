package io.github.s7i.doer.domain.helix;

import io.github.s7i.doer.DoerException;
import org.apache.helix.HelixManager;
import org.apache.helix.InstanceType;
import org.apache.helix.zookeeper.datamodel.ZNRecord;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class Participant extends HelixMember {

    CountDownLatch countDownLatch = new CountDownLatch(1);

    public Participant(String instanceName, String clusterName, String server) throws Exception {
        super(instanceName, clusterName, server);

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

        if (!engine.registerStateModelFactory(MasterSlaveModel.MODEL, new MasterSlaveModel.Factory())) {
            throw new DoerException("model not registered");
        }

        if (!engine.registerStateModelFactory(GradeStateModel.MODEL, new GradeStateModel.Factory())) {
            throw new DoerException("model not registered");
        }


        var record = new ZNRecord(UUID.randomUUID().toString());
        record.setSimpleField("X_TEST", "test of simple field");

        manager.setLiveInstanceInfoProvider(() -> record);
    }

    @Override
    protected void onAfter(HelixManager manager) {

    }
}
