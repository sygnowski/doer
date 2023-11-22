package io.github.s7i.doer.command;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.domain.ingest.IngestProcessor;
import io.github.s7i.doer.manifest.ingest.IngestRecordManifest;
import picocli.CommandLine.Command;

import java.io.File;

@Command(name = "ingest")
public class Ingest extends CommandWithContext<IngestRecordManifest> {

    @Override
    protected File getDefaultManifestFile() {
        return new File("ingest.yml");
    }

    @Override
    protected Class<IngestRecordManifest> manifestClass() {
        return IngestRecordManifest.class;
    }

    @Override
    public void onExecuteCommand(Context ctx, IngestRecordManifest manifest) {
        new IngestProcessor(ctx).process(manifest);
    }
}
