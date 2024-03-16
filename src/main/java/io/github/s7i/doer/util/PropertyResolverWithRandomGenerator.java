package io.github.s7i.doer.util;

import io.github.s7i.doer.config.Range;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Random;

@Slf4j
public class PropertyResolverWithRandomGenerator extends PropertyResolver {

    public static final String KW_RANDOM = "rand=";
    public static final String FALLBACK_VALUE = "0";
    private final Random rand = new Random();

    public PropertyResolverWithRandomGenerator(Map<String, String> rowState) {
        super(rowState);
    }

    @Override
    public String lookup(String key) {
        if (key.startsWith(KW_RANDOM)) {
            try {
                var range = new Range(key.substring(KW_RANDOM.length()));
                int max = Math.toIntExact(range.getTo());
                int min = Math.toIntExact(range.getFrom());
                int rnd = rand.nextInt((max - min) + 1) + min;
                return Integer.toString(rnd);
            } catch (Exception e) {
                log.error("while generating random number form expression: {}", key, e);
                return FALLBACK_VALUE;
            }
        }

        return super.lookup(key);
    }
}
