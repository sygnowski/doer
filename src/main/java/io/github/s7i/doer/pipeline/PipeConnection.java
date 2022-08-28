package io.github.s7i.doer.pipeline;

import io.github.s7i.doer.domain.output.Output.Load;

public interface PipeConnection {

    void push(PipePusher pusher);

    Load pull();
}
