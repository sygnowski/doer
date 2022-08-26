package io.github.s7i.doer.domain.ingest;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.domain.output.ConsoleOutput;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.manifest.ingest.IngestRecordManifest;
import io.github.s7i.doer.util.PropertyResolver;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class IngestProcessor {

    private final Context context;
    private Output output = new ConsoleOutput();

    public void process(IngestRecordManifest manifest) {

        var propertyResolver = new PropertyResolver(context.getParams());

        manifest.getRecords().stream()
                .map(rec -> Output.Load.builder()
                        .data(propertyResolver.resolve(rec.getRecord()).getBytes(StandardCharsets.UTF_8))
                        .build())
                .forEach(output::emit);
    }
}
