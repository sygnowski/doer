package io.github.s7i.doer.domain.output;

import io.github.s7i.doer.pipeline.BlockingPipePuller;
import io.github.s7i.doer.pipeline.PipeConnection;
import lombok.RequiredArgsConstructor;

import static java.util.Objects.requireNonNull;


@RequiredArgsConstructor
public class PipelineOutput implements Output {


    private final PipeConnection pipeConnection;
    private BlockingPipePuller pusher;

    @Override
    public void open() {
        pusher = new BlockingPipePuller();
        pipeConnection.registerPuller(pusher);
    }

    @Override
    public void emit(Load load) {
        requireNonNull(pusher);
        pusher.offer(load);
    }

    @Override
    public void close() throws Exception {

    }
}
