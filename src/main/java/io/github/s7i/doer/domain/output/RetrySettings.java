package io.github.s7i.doer.domain.output;

import io.github.s7i.doer.Globals;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.Getter;

@Getter
public class RetrySettings {

    public static final String RETRY_MAX_ATTEMPTS = "doer.retry.attempts";
    public static final String RETRY_WAIT_SEC = "doer.retry.wait";

    public static RetrySettings get() {
        return Holder.instance;
    }

    private static class Holder {

        static final RetrySettings instance = new RetrySettings();
    }

    private final int attempts;
    private final Duration wait;

    private RetrySettings() {
        var params = Globals.INSTANCE.getParams();

        attempts = Integer.parseInt(params.getOrDefault(RETRY_MAX_ATTEMPTS, "10"));
        wait = Duration.of(
              Integer.parseInt(params.getOrDefault(RETRY_WAIT_SEC, "30")),
              ChronoUnit.SECONDS
        );
    }
}
