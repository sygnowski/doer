package io.github.s7i.doer.pipeline;

import io.github.s7i.doer.DoerException;

public interface PipeConnection {

    default void registerPuller(PipePuller puller) {
        throw new DoerException("not implemented");
    }

    default void registerPusher(PipePusher pusher) {
        throw new DoerException("not implemented");
    }
}
