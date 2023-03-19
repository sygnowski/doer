package io.github.s7i.doer.domain.helix;

import io.github.s7i.doer.DoerException;
import io.github.s7i.doer.Globals;
import io.github.s7i.doer.domain.output.OutputBuilder;
import io.github.s7i.doer.util.PropertyResolver;
import io.github.s7i.doer.util.Utils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.helix.HelixManager;
import org.apache.helix.HelixManagerFactory;
import org.apache.helix.InstanceType;
import org.apache.helix.model.LiveInstance;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static io.github.s7i.doer.domain.helix.Utll.LISTENERS;
import static io.github.s7i.doer.domain.helix.Utll.LISTENERS_ALL;
import static java.util.Objects.isNull;


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
    private transient EventLogger eventLogger;

    {
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup, "helix-shutdown"));
    }

    public HelixMember(String instanceName, String clusterName, String server) {
        var pr = new PropertyResolver();

        this.instanceName = pr.resolve(instanceName);
        this.clusterName = pr.resolve(clusterName);
        this.server = pr.resolve(server);
    }

    public void enable() throws Exception {
        initStateLogger();
        performEnableListeners(flags.getOrDefault(LISTENERS, ""));
    }

    protected void performEnableListeners(String listeners) {
        if (Utils.hasAnyValue(listeners)) {
            Stream.of(listeners.split(","))
                    .filter(Utils::hasAnyValue)
                    .forEach(this::enableListeners);
        }
    }

    public EventLogger getEventLogger() {
        initStateLogger();
        return eventLogger;
    }

    private void enableListeners(String type) {
        initStateLogger();
        try {
            switch (type) {
                case "isl":
                    helixManager.addIdealStateChangeListener(eventLogger);
                    break;
                case "evl":
                    helixManager.addExternalViewChangeListener(eventLogger);
                    break;
                case "lil":
                    helixManager.addLiveInstanceChangeListener(eventLogger);
                    break;
                case LISTENERS_ALL:
                    helixManager.addIdealStateChangeListener(eventLogger);
                    helixManager.addExternalViewChangeListener(eventLogger);
                    helixManager.addLiveInstanceChangeListener(eventLogger);
                default:
                    log.warn("unknown listener: {}", type);
                    break;

            }
        } catch (Exception e) {
            throw new DoerException(e);
        }
    }

    private void initStateLogger() {
        synchronized (HelixMember.class) {
            if (isNull(eventLogger)) {
                eventLogger = new EventLogger();
                eventLogger.setMeta(instanceName, clusterName);
                var output = Stream.of(
                                flags.get("doer.output"),
                                System.getenv("DOER_OUTPUT")
                        ).filter(Utils::hasAnyValue)
                        .findAny().orElse("");
                if (Utils.hasAnyValue(output)) {
                    log.info("Using output: {}", output);
                    var out = new OutputBuilder().context(Globals.INSTANCE).build(() -> output);
                    out.open();
                    eventLogger.setOutput(out);
                }
            }
        }
    }

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
