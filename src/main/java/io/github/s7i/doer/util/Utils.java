package io.github.s7i.doer.util;

import static java.util.Objects.nonNull;

public abstract class Utils {

    public static boolean hasAnyValue(String str) {
        return nonNull(str) && !str.isBlank();
    }


}
