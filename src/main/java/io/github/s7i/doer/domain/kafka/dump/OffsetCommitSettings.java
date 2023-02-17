package io.github.s7i.doer.domain.kafka.dump;

import lombok.Data;
import lombok.ToString;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Data
@ToString
public class OffsetCommitSettings {

    public enum CommitType {
        SYNC, ASYNC
    }
    public static final OffsetCommitSettings DEFAULT = new OffsetCommitSettings();

    int maxPollSize = 50;
    Duration syncCommitDeadline = Duration.of(15, ChronoUnit.SECONDS);

    CommitType type= CommitType.SYNC;
}
