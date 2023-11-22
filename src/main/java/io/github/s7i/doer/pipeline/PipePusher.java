package io.github.s7i.doer.pipeline;

import io.github.s7i.doer.domain.output.Output;

public interface PipePusher {

    void offer(Output.Load load);
}
