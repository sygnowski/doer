package io.github.s7i.doer.pipeline;

import io.github.s7i.doer.domain.output.Output;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BlockingPipePuller implements PipePuller {

    public static final int SLEEP_FOR_CHANGE = 10;
    private final ArrayBlockingQueue<Output.Load> queue = new ArrayBlockingQueue<>(1);

    /**
     * Blocking call.
     *
     * @return
     */
    @Override
    public Output.Load onNextLoad() {

        Output.Load load;
        while ((load = queue.peek()) == null) {
            try {
                TimeUnit.MILLISECONDS.sleep(SLEEP_FOR_CHANGE);
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


    /**
     * Blocking call.
     *
     * @param load
     */
    public void offer(Output.Load load) {
        boolean accepted;
        do {
            accepted = queue.offer(load);
            if (!accepted) {
                try {
                    TimeUnit.MILLISECONDS.sleep(SLEEP_FOR_CHANGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        while (!accepted);
    }
}
