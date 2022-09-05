package io.github.s7i.doer.domain.output;

import io.github.s7i.doer.pipeline.BlockingPipePusher;
import io.github.s7i.doer.pipeline.PipeConnection;
import lombok.RequiredArgsConstructor;

import static java.util.Objects.requireNonNull;


@RequiredArgsConstructor
public class PipelineOutput implements Output {


    private final PipeConnection pipeConnection;
    private BlockingPipePusher pusher;

    @Override
    public void open() {
        pusher = new BlockingPipePusher();
        pipeConnection.registerPusher(pusher);
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
