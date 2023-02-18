package io.github.s7i.doer.domain.kafka.dump;

import lombok.Data;
import lombok.ToString;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Data
@ToString
public class OffsetCommitSettings {
    public static final OffsetCommitSettings DEFAULT = new OffsetCommitSettings();
    public static final String DOER_MAX_POOL_SIZE = "doer.max.pool.size";
    public static final String DOER_COMMIT_TIMEOUT = "doer.commit.timeout";

    public static OffsetCommitSettings from(Map<String, String> params) {
        try {
            var s = new OffsetCommitSettings();
            if (params.containsKey(DOER_MAX_POOL_SIZE)) {
                s.setMaxPollSize(Integer.parseInt(params.get(DOER_MAX_POOL_SIZE)));
            }
            if (params.containsKey(DOER_COMMIT_TIMEOUT)) {
                s.setSyncCommitDeadline(Duration.of(
                        Integer.parseInt(params.get(DOER_COMMIT_TIMEOUT)),
                        ChronoUnit.SECONDS)
                );
            }
            return s;
        } catch (Exception e) {
            LoggerFactory.getLogger(OffsetCommitSettings.class).error("fromParams", e);
        }
        return DEFAULT;
    }

    public enum CommitType {
        SYNC, ASYNC
    }

    int maxPollSize = 50;
    Duration syncCommitDeadline = Duration.of(15, ChronoUnit.SECONDS);

    CommitType type = CommitType.SYNC;
}
