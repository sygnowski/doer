package io.github.s7i.doer.util;

import static java.util.Objects.nonNull;

import lombok.experimental.UtilityClass;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;

@UtilityClass
public class Utils {

    public static boolean hasAnyValue(String str) {
        return nonNull(str) && !str.isBlank();
    }
    public BufferedReader resource(String path) {
        return new BufferedReader(
                new InputStreamReader(
                        Objects.requireNonNull(
                                Utils.class.getResourceAsStream(path),
                                "resource not found: " + path
                        )
                )
        );
    }

}
