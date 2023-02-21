package io.github.s7i.doer.command;

import io.github.s7i.doer.Doer;
import io.github.s7i.doer.manifest.ManifestException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

@Slf4j(topic = "doer.console")
public abstract class Command implements Callable<Integer> {
    @Setter
    private int exitCode;

    @Override
    public Integer call() {
        try {
            onExecuteCommand();
        } catch (ManifestException me) {
            exitCode = Doer.EC_INVALID_USAGE;
            log.warn(me.getMessage());
        } catch (Exception e) {
            log.error("fatal error", e);
            exitCode = Doer.EC_ERROR;
        }
        if (exitCode != 0) {
            log.warn("exit code: {} of command: {}", exitCode, this);
        }
        return exitCode;
    }
    public abstract void onExecuteCommand();
}
