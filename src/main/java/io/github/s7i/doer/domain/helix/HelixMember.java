package io.github.s7i.doer.domain.helix;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.s7i.doer.util.PropertyResolver;
import io.github.s7i.doer.util.Utils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.helix.HelixManager;
import org.apache.helix.HelixManagerFactory;
import org.apache.helix.InstanceType;
import org.apache.helix.NotificationContext;
import org.apache.helix.model.ExternalView;
import org.apache.helix.model.IdealState;
import org.apache.helix.model.LiveInstance;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Slf4j(topic = "doer.console")
public abstract class HelixMember {

    protected final String instanceName;
    protected final String clusterName;
    protected final String server;

    @Getter
    @Setter
    @Accessors(fluent = true)
    protected Map<String, String> flags = Collections.emptyMap();

    protected HelixManager helixManager;
    protected final ObjectMapper objectMapper = Utils.preetyObjectMapper();

    {
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup, "helix-shutdown"));
    }

    public HelixMember(String instanceName, String clusterName, String server) {
        var pr = new PropertyResolver();

        this.instanceName = pr.resolve(instanceName);
        this.clusterName = pr.resolve(clusterName);
        this.server = pr.resolve(server);
    }

    public abstract void enable() throws Exception;

    protected HelixManager connect(InstanceType instanceType) throws Exception {

        helixManager = HelixManagerFactory.getZKHelixManager(
                clusterName,
                instanceName,
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

    public void updateResource(String res, String value) {
        var dataAccessor = helixManager.getHelixDataAccessor();
        var key = dataAccessor.keyBuilder().liveInstance(instanceName);

        LiveInstance li = dataAccessor.getProperty(key);
        if (null != li) {
            li.setResourceCapacityMap(Map.of(res, value));

            var result = dataAccessor.updateProperty(key, li);

            log.debug("resource {} update ok: {}", key, result);
        }
    }

    public Optional<LiveInstance> getLiveInstance(String name) {
        var dataAccessor = helixManager.getHelixDataAccessor();
        var key = dataAccessor.keyBuilder().liveInstance(name);

        LiveInstance li = dataAccessor.getProperty(key);
        return Optional.ofNullable(li);
    }

}
