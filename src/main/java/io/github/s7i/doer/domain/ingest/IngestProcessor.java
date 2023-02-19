package io.github.s7i.doer.domain.ingest;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.DoerException;
import io.github.s7i.doer.domain.output.ConsoleOutput;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.manifest.ingest.IngestRecordManifest;
import io.github.s7i.doer.util.Mark;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class IngestProcessor {

    @Mark.Param
    public static final String DOER_OUTPUT = "doer.output";
    private final Context context;

    public void process(IngestRecordManifest manifest) {
        try (var output = context.getParams().entrySet().stream()
                .filter(e -> e.getKey().equals(DOER_OUTPUT))
                .map(def -> context.buildOutput(def::getValue))
                .findFirst()
                .orElseGet(ConsoleOutput::new)) {

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
