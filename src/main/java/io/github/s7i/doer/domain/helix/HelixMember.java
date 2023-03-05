package io.github.s7i.doer.domain.helix;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.s7i.doer.util.PropertyResolver;
import io.github.s7i.doer.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.helix.HelixManager;
import org.apache.helix.HelixManagerFactory;
import org.apache.helix.InstanceType;
import org.apache.helix.NotificationContext;
import org.apache.helix.model.ExternalView;
import org.apache.helix.model.IdealState;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j(topic = "doer.console")
public abstract class HelixMember {

    protected final String instanceName;
    protected final String clusterName;
    protected final String server;

    protected HelixManager helixManager;

    {
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup, "helix-shutdown"));
    }

    protected final ObjectMapper objectMapper = Utils.preetyObjectMapper();

    protected HelixManager connect(InstanceType instanceType) throws Exception {
        var pr = new PropertyResolver();
        helixManager = HelixManagerFactory.getZKHelixManager(
                pr.resolve(clusterName),
                pr.resolve(instanceName),
                instanceType,
                server);
        onBefore(helixManager);
        helixManager.connect();
        onAfter(helixManager);
        return helixManager;
    }

    protected void onBefore(HelixManager manager) {

    }

    protected void onAfter(HelixManager manager) {

    }

    public void logEv(List<ExternalView> externalViewList, NotificationContext changeContext) {
        try {
            var changeType = changeContext.getChangeType();
            var type = changeContext.getType();
            var event = Map.of(
                    "type", type.name(),
                    "changeType", changeType.name(),
                    "externalViewList", externalViewList
            );
            log.info("onExternalViewChange: \n{}", objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("oops", e);
        }
    }

    public void logIs(List<IdealState> idealState, NotificationContext changeContext) throws InterruptedException {
        try {
            var changeType = changeContext.getChangeType();
            var type = changeContext.getType();
            var event = Map.of(
                    "type", type.name(),
                    "changeType", changeType.name(),
                    "idealStateList", idealState
            );
            log.info("onIdealStateChange: \n{}", objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("oops", e);
        }
    }

    public void cleanup() {
        if (helixManager != null && helixManager.isConnected()) {
            helixManager.disconnect();
        }
        log.info("cleanup...");
    }

}
