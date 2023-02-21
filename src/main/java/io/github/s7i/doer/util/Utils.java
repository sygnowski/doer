package io.github.s7i.doer.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;

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

    public static ObjectMapper preetyObjectMapper() {
        return new ObjectMapper()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(SerializationFeature.INDENT_OUTPUT, true);
    }
}
