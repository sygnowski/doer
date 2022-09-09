package io.github.s7i.doer.domain.helix;

import static java.util.Objects.nonNull;

import java.util.Map;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.helix.InstanceType;

public class IdealStateUpdater extends HelixMember {

    @Setter
    private Map<String, String> simpleFields;
    @Setter
    private String resource;

    public IdealStateUpdater(String instanceName, String clusterName, String server) {
        super(instanceName, clusterName, server);
    }

    @SneakyThrows
    public void update() {

        var helix = connect(InstanceType.ADMINISTRATOR);
        var admin = helix.getClusterManagmentTool();
        var is = admin.getResourceIdealState(clusterName, resource);
        if (nonNull(simpleFields)) {
            is.getRecord().setSimpleFields(simpleFields);
        }
        admin.updateIdealState(clusterName, resource, is);

        helix.disconnect();
    }


}
