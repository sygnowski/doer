package io.github.s7i.doer.pipeline;

import io.github.s7i.doer.domain.output.Output;

public interface PipePuller {

    Output.Load onNextLoad();

    void onAccept();
}