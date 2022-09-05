package io.github.s7i.doer.pipeline;

import io.github.s7i.doer.domain.output.Output;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BlockingPipePusher implements PipePusher {

    private final ArrayBlockingQueue<Output.Load> queue = new ArrayBlockingQueue<>(1);

    @Override
    public Output.Load onNextLoad() {

        Output.Load load;
        while ((load = queue.peek()) == null) {
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return load;
    }

    @Override
    public void onAccept() {
        queue.poll();

    }


    public void offer(Output.Load load) {
        queue.offer(load);
    }
}
