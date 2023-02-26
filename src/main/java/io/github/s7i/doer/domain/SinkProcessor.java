package io.github.s7i.doer.domain;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.DoerException;
import io.github.s7i.doer.domain.output.DefaultOutputProvider;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.manifest.SinkManifest;
import io.github.s7i.doer.pipeline.PipePuller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@RequiredArgsConstructor
@Slf4j
public class SinkProcessor implements DefaultOutputProvider {


    final Context context;
    ExecutorService executorService;
    CountDownLatch taskToComplete;

    public void execute(List<SinkManifest.SinkSpec> specList) {

        context.lookupPipeline().ifPresent(pipeline -> {
            var puller = pipeline.connect().lookupPuller();

            var enabled = specList.stream()
                    .filter(SinkManifest.SinkSpec::isEnabled)
                    .collect(Collectors.toList());

            if (!enabled.isEmpty()) {
                enableOutputs(enabled, puller);
            } else {
                drainPipeline(puller);
            }
        });
        if (nonNull(taskToComplete)) {
            log.debug("Awaiting to complete all tasks...");

            try {
                taskToComplete.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DoerException(e);
            }
            try {
                executorService.shutdown();
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("Some timeouts...");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DoerException(e);
            }
            log.debug("All tasks done.");
        }
    }

    private void enableOutputs(List<SinkManifest.SinkSpec> specList, PipePuller puller) {
        var size = specList.size();

        log.debug("Number of output tasks: {}", size);

        taskToComplete = new CountDownLatch(size);

        var tg = new ThreadGroup("doer-output-workers");
        executorService = Executors.newFixedThreadPool(size, runnable ->
                new Thread(tg, runnable, "output-worker"));

        specList.stream()
                .map(s -> toRunnable(s, puller))
                .forEach(executorService::submit);
    }

    private Runnable toRunnable(SinkManifest.SinkSpec spec, PipePuller puller) {
        return () -> {
            loopOutput(context.buildOutput(spec::getOutput), puller);
            taskToComplete.countDown();
            log.debug("task completed {}", Thread.currentThread().getName());
        };
    }

    private void drainPipeline(PipePuller puller) {
        log.debug("Using default output.");

        var out = getDefaultOutput(context);
        loopOutput(out, puller);
    }

    public static void loopOutput(Output out, PipePuller puller) {
        Output.Load load;
        while (null != (load = puller.onNextLoad())) {
            out.emit(load);

            puller.onAccept();
        }
        LoggerFactory.getLogger(SinkManifest.class).debug("output loop ends.");
    }

}
