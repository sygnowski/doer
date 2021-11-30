package io.github.s7i.doer;

public interface ConsoleLog {

    default void info(String message) {
        Doer.CONSOLE.info(message);
    }

    default void info(String message, Object... args) {
        Doer.CONSOLE.info(message, args);
    }

}
