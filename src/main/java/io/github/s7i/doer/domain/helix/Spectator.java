package io.github.s7i.doer.domain.helix;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.helix.InstanceType;
import org.apache.helix.NotificationContext;
import org.apache.helix.api.listeners.ExternalViewChangeListener;
import org.apache.helix.api.listeners.IdealStateChangeListener;
import org.apache.helix.model.ExternalView;
import org.apache.helix.model.IdealState;

@Slf4j
public class Spectator extends HelixMember implements ExternalViewChangeListener, IdealStateChangeListener {

    public Spectator(String instanceName, String clusterName, String server) throws Exception {
        super(instanceName, clusterName, server);

        var helix = connect(InstanceType.SPECTATOR);
        //helix.addExternalViewChangeListener(this);
        helix.addIdealStateChangeListener(this);
    }

    @Override
    public void onExternalViewChange(List<ExternalView> externalViewList, NotificationContext changeContext) {
        logEv(externalViewList, changeContext);
    }

    @Override
    public void onIdealStateChange(List<IdealState> idealState, NotificationContext changeContext) throws InterruptedException {
        logIs(idealState, changeContext);
    }
}
