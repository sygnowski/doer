package io.github.s7i.doer.domain.helix;

import lombok.Setter;
import org.apache.helix.InstanceType;

import java.util.Map;

import static java.util.Objects.nonNull;

public class IdealStateUpdater extends HelixMember {

    @Setter
    private Map<String, String> simpleFields;
    @Setter
    private String resource;

    public IdealStateUpdater(String instanceName, String clusterName, String server) {
        super(instanceName, clusterName, server);
    }


    @Override
    public void enable() throws Exception {

        connect(InstanceType.ADMINISTRATOR);
        var admin = helixManager.getClusterManagmentTool();
        var is = admin.getResourceIdealState(clusterName, resource);
        if (nonNull(simpleFields)) {
            is.getRecord().setSimpleFields(simpleFields);
        }
        admin.updateIdealState(clusterName, resource, is);

        helixManager.disconnect();
    }


}
