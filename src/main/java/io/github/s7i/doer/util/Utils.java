package io.github.s7i.doer.util;

import static java.util.Objects.nonNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
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
    public boolean readResource(String path, Consumer<BufferedReader> brConsumer) {
        try (var br = resource(path)) {
            brConsumer.accept(br);
        } catch (Exception e) {
            log.error("oops", e);
            return false;
        }
        return true;
    }
}
