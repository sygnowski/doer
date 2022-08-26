package io.github.s7i.doer.pipeline;

import io.github.s7i.doer.domain.output.Output.Load;

public interface LoadPipe {

    void push(Load load);

    Load pull();
}
