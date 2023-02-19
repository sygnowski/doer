package io.github.s7i.doer.command;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.domain.ingest.IngestProcessor;
import io.github.s7i.doer.manifest.ingest.IngestRecordManifest;
import picocli.CommandLine.Command;

import java.io.File;

@Command(name = "ingest")
public class Ingest extends ManifestFileCommand {

    @Override
    protected File getDefaultManifestFile() {
        return new File("ingest.yml");
    }


    @Override
    public void onExecuteCommand() {

        var manifest = parseYaml(IngestRecordManifest.class);

        var ctx = new Context.Initializer(Context.InitialParameters.builder()
                .workDir(yaml.toPath().toAbsolutePath().getParent())
                .params(manifest.getParams())
                .build())
                .context();

        new IngestProcessor(ctx).process(manifest);
    }
}
