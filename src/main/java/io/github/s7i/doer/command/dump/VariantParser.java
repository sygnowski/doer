package io.github.s7i.doer.command.dump;

import static java.util.Objects.nonNull;

import io.github.s7i.doer.flow.Variant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import picocli.CommandLine.ITypeConverter;

public class VariantParser implements ITypeConverter<Variant> {

    @Override
    public Variant convert(String value) throws Exception {
        if (hasValue(value)) {
            var storage = new HashMap<String, Iterable<String>>();
            var parts = value.split("\\|");
            for (var p : parts) {
                parsePropertyVariant(p, storage);
            }
            return new Variant(storage);
        }
        return null;
    }

    private void parsePropertyVariant(String raw, Map<String, Iterable<String>> storage) {
        if (hasValue(raw)) {
            var parts = raw.split("\\=");
            var values = parts[1].split("\\,");
            if (parts.length == 2) {
                storage.put(parts[0], Arrays.asList(values));
            }
        }
    }

    private boolean hasValue(String raw) {
        return nonNull(raw) && !raw.isBlank();
    }
}
