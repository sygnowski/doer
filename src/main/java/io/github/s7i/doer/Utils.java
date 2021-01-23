package io.github.s7i.doer;

import static java.util.Objects.nonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;

public abstract class Utils {

    public static boolean hasAnyValue(String str) {
        return nonNull(str) && !str.isBlank();
    }

    @RequiredArgsConstructor
    public static class PropertyResolver implements StringLookup {

        private final Map<String, String> propertyMap;
        private final StringSubstitutor sysSubstitutor = StringSubstitutor.createInterpolator();
        private final StringSubstitutor substitutor = new StringSubstitutor(this);

        public PropertyResolver() {
            propertyMap = new HashMap<>();
        }

        public void addProperty(String name, String value) {
            propertyMap.put(resolve(name), resolve(value));
        }

        @Override
        public String lookup(String key) {
            switch (key) {
                case "__UUID":
                    return UUID.randomUUID().toString();
            }
            return propertyMap.get(key);
        }

        public String resolve(String input) {
            return sysSubstitutor.replace(substitutor.replace(input));
        }
    }


}
