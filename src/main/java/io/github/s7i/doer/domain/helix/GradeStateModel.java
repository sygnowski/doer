package io.github.s7i.doer.domain.helix;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.helix.NotificationContext;
import org.apache.helix.model.LiveInstance;
import org.apache.helix.model.Message;
import org.apache.helix.participant.statemachine.StateModel;
import org.apache.helix.participant.statemachine.StateModelFactory;
import org.apache.helix.participant.statemachine.StateModelInfo;
import org.apache.helix.participant.statemachine.Transition;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.s7i.doer.domain.helix.Utll.asKey;

@Slf4j
@StateModelInfo(initialState = "OFFLINE", states = {"OFFLINE", GradeStateModel.ALPHA, GradeStateModel.BETA, GradeStateModel.GAMMA})
public class GradeStateModel extends StateModel {
    public static final String MODEL = "GradeModel";
    public static final String ALPHA = "ALPHA";
    public static final String BETA = "BETA";
    public static final String GAMMA = "GAMMA";

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

    @Data
    public static class GoldInfo implements Comparable<GoldInfo> {

        public GoldInfo(LiveInstance li) {
            instance = li;
            var resourceCapacityMap = li.getResourceCapacityMap();
            if (resourceCapacityMap != null && resourceCapacityMap.containsKey("gold")) {
                goldLeve = Long.parseLong(resourceCapacityMap.get("gold"));
            }
        }

        public boolean hasGoldLevel() {
            return goldLeve != null;
        }

        LiveInstance instance;
        Long goldLeve = 0L;

        @Override
        public int compareTo(@NotNull GoldInfo goldInfo) {
            return goldLeve.compareTo(goldInfo.getGoldLeve());
        }
    }

    @Data
    @RequiredArgsConstructor
    public static class GoldScore {
        TreeSet<GoldInfo> set = new TreeSet<>();
        final Long lvl;

        public void offer(GoldInfo goldInfo) {
            set.add(goldInfo);
        }

        public Optional<GoldInfo> getAlpha() {
            return set.descendingSet()
                    .stream()
                    .filter(i -> i.getGoldLeve() > lvl)
                    .peek(beta -> log.info("selected alpha: {}", beta))
                    .findFirst();
        }

        public List<GoldInfo> getBeta(int limit) {
            return set.descendingSet()
                    .stream()
                    .filter(i -> i.getGoldLeve() > lvl)
                    .skip(1)
                    .limit(limit)
                    .peek(beta -> log.info("selected beta: {}", beta))
                    .collect(Collectors.toList());
        }
    }

    final SwitchStateLogger stateLogger = new SwitchStateLogger();

    @Transition(from = "OFFLINE", to = GAMMA)
    public void toGammaFromOffline(Message msg, NotificationContext context) {
        stateLogger.logSwitchState(msg, context);
    }

    @Transition(from = GAMMA, to = BETA)
    public void toBetaFromGamma(Message msg, NotificationContext context) {
        stateLogger.logSwitchState(msg, context);
    }

    @Transition(from = BETA, to = ALPHA)
    public void toAlphaFromBeta(Message msg, NotificationContext context) {
        stateLogger.logSwitchState(msg, context);
    }


}
