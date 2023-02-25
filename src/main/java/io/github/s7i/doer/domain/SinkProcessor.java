package io.github.s7i.doer.domain;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.manifest.SinkManifest;
import io.github.s7i.doer.pipeline.PipePuller;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SinkProcessor {

    static class SinkPipePuller implements PipePuller {

        @Override
        public Output.Load onNextLoad() {
            return null;
        }

        @Override
        public void onAccept() {

        }
    }

    final Context context;
    private SinkPipePuller puller;

    public void execute(SinkManifest.SinkSpec sinkSpec) {
        puller = new SinkPipePuller();

        context.lookupPipeline().ifPresent(pipeline -> {
            pipeline.connect("sink").registerPuller(puller);
        });
    }

}
