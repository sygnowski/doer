package io.github.s7i.doer.domain.ingest;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.DoerException;
import io.github.s7i.doer.domain.output.DefaultOutputProvider;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.manifest.ingest.IngestRecordManifest;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class IngestProcessor implements DefaultOutputProvider {

    private final Context context;

    public void process(IngestRecordManifest manifest) {
        try (var output = getDefaultOutput(context)) {

            manifest.getRecords().stream()
                    .map(rec -> Output.Load.builder()
                            .data(context.getPropertyResolver()
                                    .resolve(rec.getRecord())
                                    .getBytes(StandardCharsets.UTF_8))
                            .build())
                    .forEach(output::emit);
        } catch (Exception e) {
            throw new DoerException(e);
        }
    }
}
