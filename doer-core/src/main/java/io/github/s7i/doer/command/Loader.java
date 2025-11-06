package io.github.s7i.doer.command;

import io.github.s7i.doer.MissingDependencies;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Slf4j
public class Loader {

    private final List<String> notLoaded = new LinkedList<>();

    public String notLoadedRemarks() {
        return notLoaded.isEmpty()
              ? ""
              : "Not Loaded Commands:"
                    + "\n"
                    + String.join("\n", notLoaded)
                    + "\n";
    }

    private void notLoaded(String className) {
        notLoaded.add("%s - checks logs...".formatted(className));
    }

    public void addCommand(String className, BiConsumer<String, Object> consumer) {
        try {
            Class<?> clazz = Class.forName(className);
            if (clazz.isAnnotationPresent(Command.class)) {
                var commandName = clazz.getAnnotation(Command.class).name();
                try {
                    Object instance = clazz.getConstructor().newInstance();
                    consumer.accept(commandName, instance);
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof MissingDependencies why) {
                        notLoaded.add("%s (%s) - why: %s".formatted(commandName, className, why.getMessage()));
                        log.debug("{}/{} - skipping command due missing dependencies", commandName, className, why);
                    } else {
                        log.warn("loading command", e);
                        notLoaded(className);
                    }
                }
            } else {
                log.error("not a command: {}", className);
                notLoaded(className);
            }
        } catch (Throwable e) {
            log.warn("loading command", e);
            notLoaded(className);
        }
    }
}
