package io.github.s7i.doer.util;

import io.github.s7i.doer.Context;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@RequiredArgsConstructor
@Slf4j
public class PropertyResolver implements StringLookup, SpecialExpressionResolver {

    public static final String DOER_CALL = "doer:";
    private final Map<String, String> propertyMap;
    private final StringSubstitutor sysSubstitutor = StringSubstitutor.createInterpolator().setEnableSubstitutionInVariables(true);
    private final StringSubstitutor substitutor = new StringSubstitutor(this).setEnableSubstitutionInVariables(true);

    public PropertyResolver(Context context) {
        this(context.getParams());
    }

    @Setter
    private Function<String, String> handle;

    public PropertyResolver() {
        propertyMap = new HashMap<>();
    }

    public void addProperty(String name, String value) {
        propertyMap.put(resolve(name), resolve(value));
    }

    @Override
    public String lookup(String key) {
        if (nonNull(handle) && key.startsWith(DOER_CALL)) {
            return handle.apply(key);
        }
        var exp = lookupSpecialExpression(key);
        if (exp.isPresent()) {
            return exp.get();
        }
        if (isNull(propertyMap)) {
            return null;
        }
        return propertyMap.get(key);
    }

    public String resolve(String input) {
        return sysSubstitutor.replace(substitutor.replace(input));
    }
}
