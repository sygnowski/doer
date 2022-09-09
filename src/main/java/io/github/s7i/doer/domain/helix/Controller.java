package io.github.s7i.doer.domain.helix;

import org.apache.helix.InstanceType;

public class Controller extends HelixMember {

    public Controller(String instanceName, String clusterName, String server) throws Exception {
        super(instanceName, clusterName, server);

        connect(InstanceType.CONTROLLER);
    }
}
