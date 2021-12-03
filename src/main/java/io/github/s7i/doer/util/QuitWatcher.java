package io.github.s7i.doer.util;

import io.github.s7i.doer.ConsoleLog;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QuitWatcher implements ConsoleLog {

    public interface OnQuit {

        void doQuit();
    }

    final Executor quitExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "quit"));

    public void watchForQuit(OnQuit onQuit) {
        final var console = System.console();
        quitExecutor.execute(() -> {
            while (true) {
                char[] one = new char[1];
                try {
                    var len = console.reader().read(one);
                    if (len > 0) {
                        char c = one[0];
                        if (c == 'c' || c == 'q') {
                            log.debug("do quit");

                            onQuit.doQuit();
                            info("quiting");
                            break;
                        }
                    }
                } catch (IOException e) {
                    log.error("err", e);
                }
            }
        });
    }

}
