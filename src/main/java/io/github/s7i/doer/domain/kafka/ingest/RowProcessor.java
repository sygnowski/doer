package io.github.s7i.doer.domain.kafka.ingest;

import io.github.s7i.doer.manifest.ingest.TemplateProp;
import io.github.s7i.doer.manifest.ingest.ValueTemplate;
import io.github.s7i.doer.util.PropertyResolver;
import io.github.s7i.doer.util.PropertyResolverWithRandomGenerator;
import io.github.s7i.doer.util.SpecialExpression;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class RowProcessor {

    final Map<String, String> rowState = new HashMap<>();
    PropertyResolver resolver = new PropertyResolverWithRandomGenerator(rowState);
    long rowNum;
    final List<String> attributes;

    public String resolve(String input) {
        return resolver.resolve(input);
    }

    RowProcessor updateTemplateProperties(ValueTemplate valueTemplate) {
        requireNonNull(valueTemplate);
        valueTemplate
                .getProperties()
                .forEach(this::addProperty);
        return this;
    }

    public void addProperty(TemplateProp prop) {
        resolver.addProperty(prop.getName(), prop.getValue());
    }

    RowProcessor nextRowValues(List<String> values) {
        rowState.put(SpecialExpression.ROW_ID.KEY_WORD, String.valueOf(++rowNum));
        for (int pos = 0; pos < attributes.size(); pos++) {
            var value = resolver.resolve(values.get(pos));
            rowState.put(attributes.get(pos), value);
        }

        return this;
    }
}
