package io.github.s7i.doer.domain.helix;

import lombok.extern.slf4j.Slf4j;
import org.apache.helix.NotificationContext;
import org.apache.helix.model.Message;
import org.apache.helix.participant.statemachine.StateModel;
import org.apache.helix.participant.statemachine.StateModelFactory;
import org.apache.helix.participant.statemachine.StateModelInfo;
import org.apache.helix.participant.statemachine.Transition;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.github.s7i.doer.domain.helix.Utll.asKey;

@Slf4j
@StateModelInfo(initialState = "OFFLINE", states = {"OFFLINE", "ALPHA", "BETA", "GAMMA"})
public class GradeStateModel extends StateModel {
    public static final String MODEL = "GradeModel";

    public static class Factory extends StateModelFactory<GradeStateModel> {
        Map<String, GradeStateModel> register = new LinkedHashMap<>();

        @Override
        public GradeStateModel createAndAddStateModel(String resourceName, String partitionKey) {
            var model = new GradeStateModel();
            register.put(asKey(resourceName, partitionKey), model);
            return model;
        }


        @Override
        public GradeStateModel getStateModel(String resourceName, String partitionKey) {
            return register.get(asKey(resourceName, partitionKey));
        }
    }

    final SwitchStateLogger stateLogger = new SwitchStateLogger();

    @Transition(from = "OFFLINE", to = "GAMMA")
    public void toGammaFromOffline(Message msg, NotificationContext context) {
        stateLogger.logSwitchState(msg, context);
    }

    @Transition(from = "GAMMA", to = "BETA")
    public void toBetaFromGamma(Message msg, NotificationContext context) {
        stateLogger.logSwitchState(msg, context);
    }

    @Transition(from = "BETA", to = "ALPHA")
    public void toAlphaFromBeta(Message msg, NotificationContext context) {
        stateLogger.logSwitchState(msg, context);
    }


}