package io.github.s7i.doer.domain.output;

import io.github.s7i.doer.pipeline.LoadPipe;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class PipelineOutput implements Output {


    private final LoadPipe loadPipe;

    @Override
    public void open() {

    }

    @Override
    public void emit(Load load) {
        loadPipe.push(load);
    }

    @Override
    public void close() throws Exception {

    }
}
