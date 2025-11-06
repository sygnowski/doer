package io.github.s7i.doer;

public class MissingDependencies extends DoerException {

    public MissingDependencies(String message) {
        super(message);
    }

    public MissingDependencies(Throwable cause) {
        super(cause);
    }
}
