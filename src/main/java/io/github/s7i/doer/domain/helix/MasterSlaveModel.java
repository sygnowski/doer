package io.github.s7i.doer.domain.helix;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.helix.NotificationContext;
import org.apache.helix.model.MasterSlaveSMD;
import org.apache.helix.model.Message;
import org.apache.helix.participant.statemachine.StateModel;
import org.apache.helix.participant.statemachine.StateModelFactory;
import org.apache.helix.participant.statemachine.StateModelInfo;
import org.apache.helix.participant.statemachine.Transition;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.github.s7i.doer.domain.helix.Utll.asKey;

@RequiredArgsConstructor
@Slf4j(topic = "doer.console")
@StateModelInfo(initialState = "OFFLINE", states = {"OFFLINE", "MASTER", "SLAVE"})
@ToString
public class MasterSlaveModel extends StateModel {

    public static final String MODEL = MasterSlaveSMD.name;

    public static class Factory extends StateModelFactory<MasterSlaveModel> {

        Map<String, MasterSlaveModel> register = new LinkedHashMap<>();

        @Override
        public MasterSlaveModel createAndAddStateModel(String resourceName, String partitionKey) {
            var model = new MasterSlaveModel(resourceName, partitionKey);
            log.info("model created: {}", model);
            register.put(asKey(resourceName, partitionKey), model);
            return model;
        }

        @Override
        public MasterSlaveModel getStateModel(String resourceName, String partitionKey) {
            return register.get(asKey(resourceName, partitionKey));
        }
    }

    final String resourceName;
    final String partitionName;

    final SwitchStateLogger stateLogger = new SwitchStateLogger();

    @Transition(from = "OFFLINE", to = "SLAVE")
    public void toSlaveFromOffline(Message msg, NotificationContext context) {
        stateLogger.logSwitchState(msg, context);

    }

    @Transition(from = "SLAVE", to = "MASTER")
    public void toMasterFromSlave(Message msg, NotificationContext context) {
        stateLogger.logSwitchState(msg, context);

    }
}
