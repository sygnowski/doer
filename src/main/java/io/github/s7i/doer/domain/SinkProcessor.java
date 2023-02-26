package io.github.s7i.doer.domain;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.domain.output.DefaultOutputProvider;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.manifest.SinkManifest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SinkProcessor implements DefaultOutputProvider {


    final Context context;

    public void execute(SinkManifest.SinkSpec sinkSpec) {

        context.lookupPipeline().ifPresent(pipeline -> {
            var puller = pipeline.connect("sink").lookupPuller();

            Output.Load load;
            while (null != (load = puller.onNextLoad())) {
                getDefaultOutput(context).emit(load);

                puller.onAccept();
            }
        });

    }

}
