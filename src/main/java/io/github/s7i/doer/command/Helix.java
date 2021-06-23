package io.github.s7i.doer.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.helix.HelixManagerFactory;
import org.apache.helix.InstanceType;
import org.apache.helix.NotificationContext;
import org.apache.helix.api.listeners.ExternalViewChangeListener;
import org.apache.helix.api.listeners.IdealStateChangeListener;
import org.apache.helix.model.ExternalView;
import org.apache.helix.model.IdealState;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "helix", description = "Runs the Helix Spectator")
@Slf4j
public class Helix implements Runnable, ExternalViewChangeListener, IdealStateChangeListener {

    @Option(names = "-s", defaultValue = "localhost:2181")
    String server;

    @Option(names = "-c")
    String clusterName;

    @Option(names = "-n", defaultValue = "doer")
    String instanceName;

    ObjectMapper objectMapper = new ObjectMapper()
          .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
          .configure(SerializationFeature.INDENT_OUTPUT, true);

    @SneakyThrows
    @Override
    public void run() {
        log.info("running a helix spectator...");
        runSpectator();
    }

    private void runSpectator() throws Exception {
        var helix = HelixManagerFactory.getZKHelixManager(clusterName,
              instanceName,
              InstanceType.SPECTATOR,
              server);
        helix.connect();

        helix.addExternalViewChangeListener(this);
        helix.addIdealStateChangeListener(this);
    }


    @Override
    public void onExternalViewChange(List<ExternalView> externalViewList, NotificationContext changeContext) {
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

    @Override
    public void onIdealStateChange(List<IdealState> idealState, NotificationContext changeContext) throws InterruptedException {
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
