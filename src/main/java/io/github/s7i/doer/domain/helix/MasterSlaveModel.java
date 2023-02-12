package io.github.s7i.doer.domain.helix;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.s7i.doer.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.helix.NotificationContext;
import org.apache.helix.model.MasterSlaveSMD;
import org.apache.helix.model.Message;
import org.apache.helix.participant.statemachine.StateModel;
import org.apache.helix.participant.statemachine.StateModelFactory;
import org.apache.helix.participant.statemachine.StateModelInfo;
import org.apache.helix.participant.statemachine.Transition;

import java.util.Map;

@RequiredArgsConstructor
@Slf4j(topic = "doer.console")
@StateModelInfo(initialState = "OFFLINE", states = {"OFFLINE", "MASTER", "SLAVE"})
@ToString
public class MasterSlaveModel extends StateModel {

    public static final String MODEL = MasterSlaveSMD.name;

    public static class Factory extends StateModelFactory<MasterSlaveModel> {
        @Override
        public MasterSlaveModel createNewStateModel(String resourceName, String partitionName) {
            var model = new MasterSlaveModel(resourceName, partitionName);
            log.info("model created: {}", model);
            return model;
        }
    }

    final String resourceName;
    final String partitionName;

    protected final ObjectMapper objectMapper = Utils.preetyObjectMapper();


    @Transition(from = "OFFLINE", to = "SLAVE")
    public void toSlaveFromOffline(Message msg, NotificationContext context) {
        logSwitchState(msg, context);

    }

    @Transition(from = "SLAVE", to = "MASTER")
    public void toMasterFromSlave(Message msg, NotificationContext context) {
        logSwitchState(msg, context);

    }

    @SneakyThrows
    void logSwitchState(Message msg, NotificationContext context) {
        var changeType = context.getChangeType();
        var type = context.getType();
        var event = Map.of(
                "type", type.name(),
                "changeType", changeType.name(),
                "msg", msg
        );
        log.info("switch-state: {}\n", objectMapper.writeValueAsString(event));
    }

}
