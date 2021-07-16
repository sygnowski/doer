package io.github.s7i.doer.domain.helix;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.helix.HelixManager;
import org.apache.helix.HelixManagerFactory;
import org.apache.helix.InstanceType;
import org.apache.helix.NotificationContext;
import org.apache.helix.model.ExternalView;
import org.apache.helix.model.IdealState;

@RequiredArgsConstructor
@Slf4j
public abstract class HelixMember {

    protected final String instanceName;
    protected final String clusterName;
    protected final String server;

    protected ObjectMapper objectMapper = new ObjectMapper()
          .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
          .configure(SerializationFeature.INDENT_OUTPUT, true);


    protected HelixManager connect(InstanceType instanceType) throws Exception {
        var helix = HelixManagerFactory.getZKHelixManager(
              clusterName,
              instanceName,
              instanceType,
              server);
        onBefore(helix);
        helix.connect();
        onAfter(helix);
        return helix;
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

}
