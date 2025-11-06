package io.github.s7i.doer;

import static io.github.s7i.doer.Constrains.CONSOLE;

public interface ConsoleLog {

    default void info(String message) {
        CONSOLE.info(message);
    }

    default void info(String message, Object one) {
        CONSOLE.info(message, one);
    }

    default void info(String message, Object one, Object two) {
        CONSOLE.info(message, one, two);
    }

//    default void info(String message, Object... args) {
//        Doer.CONSOLE.info(message, args);
//    }

}
