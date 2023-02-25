package io.github.s7i.doer.command;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.domain.SinkProcessor;
import io.github.s7i.doer.manifest.SinkManifest;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(name = "sink")
public class SinkCommand extends CommandWithContext<SinkManifest> {

    @Override
    protected File getDefaultManifestFile() {
        return new File("sink.yml");
    }

    @Override
    protected Class<SinkManifest> manifestClass() {
        return SinkManifest.class;
    }

    @Override
    public void onExecuteCommand(Context context, SinkManifest manifest) {
        new SinkProcessor(context).execute(manifest.getSpec().stream().findFirst().orElseThrow());
    }
}
