package io.github.s7i.doer.domain.ingest;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.DoerException;
import io.github.s7i.doer.domain.output.DefaultOutputProvider;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.manifest.ingest.IngestRecordManifest;
import io.github.s7i.doer.util.PropertyResolver;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class IngestProcessor implements DefaultOutputProvider {

    private final Context context;
    private PropertyResolver propertyResolver;

    public void process(IngestRecordManifest manifest) {
        propertyResolver = context.getPropertyResolver();

        try (var output = getDefaultOutput(context)) {

            manifest.getRecords().stream()
                    .map(this::resolveRecord)
                    .forEach(output::emit);
        } catch (Exception e) {
            throw new DoerException(e);
        }
    }

    private Output.Load resolveRecord(IngestRecordManifest.Record record) {
        final var bld = Output.Load.builder();

        Optional.ofNullable(record.getRecord()).ifPresent(data ->
                bld.data(propertyResolver
                        .resolve(record.getRecord())
                        .getBytes(StandardCharsets.UTF_8))
        );

        Optional.ofNullable(record.getKey()).ifPresent(key ->
                bld.key(propertyResolver.resolve(record.getKey()))
        );
        bld.meta(Map.of("doer.record.type", "ingest-record"));

        return bld.build();
    }
}
