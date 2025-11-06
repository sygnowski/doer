package io.github.s7i.doer.util;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.s7i.doer.DoerException;
import io.github.s7i.doer.command.Loader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class Utils {

    public static final String COMMON_ERROR = Emoji.BOOP.get() + " oops... ";

    @RequiredArgsConstructor
    public enum Emoji {
        RAINBOW("1F308"),
        BOOP("1f4a9");

        public String get() {
            return Character.toString(Integer.parseInt(this.utfCode, 16));
        }

        private final String utfCode;
    }

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
            log.error(COMMON_ERROR, e);
            return false;
        }
        return true;
    }

    public static ObjectMapper preetyObjectMapper() {
        return new ObjectMapper()
              .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
              .configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    public String loadCommand(BiConsumer<String, Object> consumer) {
        var loader = new Loader();
        try (var cmds = Utils.resource("/cmds/list")) {
            cmds.lines()
                  .forEach(clazz -> loader.addCommand(clazz, consumer));
        } catch (Exception e) {
            throw new DoerException(e);
        }
        return loader.notLoadedRemarks();
    }
}
