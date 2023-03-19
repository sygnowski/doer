package io.github.s7i.doer.domain.helix;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.util.Utils;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.helix.NotificationContext;
import org.apache.helix.api.listeners.ExternalViewChangeListener;
import org.apache.helix.api.listeners.IdealStateChangeListener;
import org.apache.helix.api.listeners.LiveInstanceChangeListener;
import org.apache.helix.model.ExternalView;
import org.apache.helix.model.IdealState;
import org.apache.helix.model.LiveInstance;
import org.apache.helix.model.Message;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

@Slf4j(topic = "doer.console")
public class EventLogger implements ExternalViewChangeListener, IdealStateChangeListener, LiveInstanceChangeListener {

    public static class Event {
        protected final Map<String, Object> attributes = new LinkedHashMap<>();

        public Event() {
            attributes.put("timestamp", Instant.now().toString());
        }

        Event on(Message msg, NotificationContext context) {

            var changeType = context.getChangeType();
            var type = context.getType();

            attributes.putAll(Map.of(
                    "type", type.name(),
                    "changeType", changeType.name(),
                    "message", msg
            ));

            return this;
        }

        Event on(List<ExternalView> externalViewList, NotificationContext changeContext) {

            var changeType = changeContext.getChangeType();
            var type = changeContext.getType();
            attributes.putAll(Map.of(
                    "type", type.name(),
                    "changeType", changeType.name(),
                    "externalViewList", externalViewList
            ));
            return this;
        }

        Event onIdeal(List<IdealState> idealState, NotificationContext changeContext) {

            var changeType = changeContext.getChangeType();
            var type = changeContext.getType();
            attributes.putAll(Map.of(
                    "type", type.name(),
                    "changeType", changeType.name(),
                    "idealStateList", idealState
            ));
            return this;
        }

        public Event onLive(List<LiveInstance> liveInstances, NotificationContext changeContext) {
            var changeType = changeContext.getChangeType();
            var type = changeContext.getType();
            attributes.putAll(Map.of(
                    "type", type.name(),
                    "changeType", changeType.name(),
                    "liveInstancesList", liveInstances
            ));
            return this;
        }
    }

    protected final ObjectMapper objectMapper = Utils.preetyObjectMapper();
    @Setter
    protected Output output;
    private Map<String, String> meta = Collections.emptyMap();

    public void setMeta(String instanceName, String clusterName) {
        meta = new LinkedHashMap<>();
        meta.put("instanceName", instanceName);
        meta.put("clusterName", clusterName);
    }

    @Override
    public void onExternalViewChange(List<ExternalView> externalViewList, NotificationContext changeContext) {
        report(new Event().on(externalViewList, changeContext));
    }

    @Override
    public void onIdealStateChange(List<IdealState> idealState, NotificationContext changeContext) throws InterruptedException {
        report(new Event().onIdeal(idealState, changeContext));
    }

    @Override
    public void onLiveInstanceChange(List<LiveInstance> liveInstances, NotificationContext changeContext) {
        report(new Event().onLive(liveInstances, changeContext));
    }

    public void logSwitchState(Message msg, NotificationContext context) {
        report(new Event().on(msg, context));
    }


    @SneakyThrows
    void report(Event event) {
        var attributes = event.attributes;
        attributes.putAll(meta);

        if (isNull(output)) {
            log.info("helix event: {}", objectMapper.writeValueAsString(attributes));
        } else {
            output.emit(Output.Load.builder()
                    .data(objectMapper.writeValueAsBytes(attributes))
                    .build()
            );
        }
    }
}
