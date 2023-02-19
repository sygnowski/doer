package io.github.s7i.doer.domain.kafka.dump;

import io.github.s7i.doer.config.Range;
import io.github.s7i.doer.util.Mark;
import lombok.Data;
import lombok.ToString;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

@Data
@ToString
public class OffsetCommitSettings {
    public static final OffsetCommitSettings DEFAULT = new OffsetCommitSettings();
    @Mark.Param
    public static final String DOER_MAX_POOL_SIZE = "doer.max.pool.size";
    @Mark.Param
    public static final String DOER_COMMIT_TIMEOUT = "doer.commit.timeout";
    @Mark.Param
    public static final String DOER_COMMIT_KIND = "doer.commit.kind";

    public static boolean isEnabled(Map<String, String> params, Collection<Range> ranges) {
        Predicate<Map<String, String>> hasParam = m -> {
            try {
                if (m.containsKey(DOER_COMMIT_KIND)) {
                    var v = m.get(DOER_COMMIT_KIND);
                    switch (CommitKind.valueOf(v.toUpperCase())) {
                        case ASYNC:
                        case SYNC:
                            return true;
                        default:
                            break;
                    }
                }
            } catch (Exception e) {
                LoggerFactory.getLogger(OffsetCommitSettings.class).error("isEnabled", e);
            }
            return false;
        };
        return hasParam.test(params) || ranges.stream().anyMatch(Range::hasTo);
    }

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
            if (params.containsKey(DOER_COMMIT_KIND)) {
                s.setKind(CommitKind.valueOf(params.get(DOER_COMMIT_KIND).toUpperCase()));
            }
            return s;
        } catch (Exception e) {
            LoggerFactory.getLogger(OffsetCommitSettings.class).error("fromParams", e);
        }
        return DEFAULT;
    }

    public boolean canMakeCommits() {
        return CommitKind.OFF != kind;
    }

    public enum CommitKind {
        SYNC, ASYNC, OFF
    }

    int maxPollSize = 50;
    Duration syncCommitDeadline = Duration.of(15, ChronoUnit.SECONDS);

    CommitKind kind = CommitKind.SYNC;
}
