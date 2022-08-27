package io.github.s7i.doer.util;

import static java.util.Objects.nonNull;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Utils {

    public static boolean hasAnyValue(String str) {
        return nonNull(str) && !str.isBlank();
    }

}
