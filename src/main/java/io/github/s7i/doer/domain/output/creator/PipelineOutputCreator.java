package io.github.s7i.doer.domain.output.creator;

import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.domain.output.PipelineOutput;
import io.github.s7i.doer.pipeline.LoadPipe;

public interface PipelineOutputCreator extends OutputCreator {


    LoadPipe getLoadPipe();

    @Override
    default Output create() {
        return new PipelineOutput(getLoadPipe());
    }
}
