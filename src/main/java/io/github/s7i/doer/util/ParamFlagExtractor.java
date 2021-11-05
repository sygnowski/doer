package io.github.s7i.doer.util;

import io.github.s7i.doer.Doer;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;

import static java.util.Objects.nonNull;

public interface ParamFlagExtractor {

    Map<String, String> getParams();

    default boolean hasFlag(String flag) {
        final var flags = getParams().get(Doer.FLAGS);

        if (nonNull(flags)) {
            if (flags.equals(flag)) {
                LoggerFactory.getLogger(Doer.class).debug("ON FLAG: {}", flag);
                return true;
            }

            final var split = flags.split("\\,");
            final var hasFlag = Arrays.stream(split)
                  .filter(Predicate.not(String::isBlank))
                  .anyMatch(flag::equals);

            if (hasFlag) {
                LoggerFactory.getLogger(Doer.class).debug("ON FLAG: {}", flag);
            }
            return hasFlag;
        }
        return false;
    }
}
