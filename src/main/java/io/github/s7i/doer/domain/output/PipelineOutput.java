package io.github.s7i.doer.domain.output;

import io.github.s7i.doer.pipeline.BlockingPipe;
import io.github.s7i.doer.pipeline.PipeConnection;
import lombok.RequiredArgsConstructor;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class PipelineOutput implements Output {


    private final PipeConnection pipeConnection;
    private BlockingPipe pusher;

    @Override
    public void open() {
        if (nonNull(pusher)) {
            return;
        }
        pusher = new BlockingPipe();
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
