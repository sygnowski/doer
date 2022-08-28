package io.github.s7i.doer.domain.output;

import io.github.s7i.doer.pipeline.PipeConnection;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class PipelineOutput implements Output {


    private final PipeConnection pipeConnection;

    @Override
    public void open() {

    }

    @Override
    public void emit(Load load) {
        pipeConnection.push(() -> load);
    }

    @Override
    public void close() throws Exception {

    }
}
