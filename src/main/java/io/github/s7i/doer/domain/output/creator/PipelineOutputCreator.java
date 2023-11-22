package io.github.s7i.doer.domain.output.creator;

import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.domain.output.PipelineOutput;
import io.github.s7i.doer.pipeline.PipeConnection;

public interface PipelineOutputCreator extends OutputCreator {


    PipeConnection getLoadPipe();

    @Override
    default Output create() {
        var pipelineOutput = new PipelineOutput(getLoadPipe());
        pipelineOutput.open();
        return pipelineOutput;
    }
}
