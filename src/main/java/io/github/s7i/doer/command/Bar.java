package io.github.s7i.doer.command;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import me.tongfei.progressbar.ProgressBar;
import picocli.CommandLine.Command;

@Command(name = "bar")
public class Bar implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        var max = 10;
        try (var bar = new ProgressBar("test", max)) {
            for (int i = 0; i < max; i++) {
                bar.step();
                TimeUnit.SECONDS.sleep(1);
            }
        }
        return 0;
    }
}
