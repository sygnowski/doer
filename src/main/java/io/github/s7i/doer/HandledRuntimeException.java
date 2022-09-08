package io.github.s7i.doer;

public class HandledRuntimeException extends RuntimeException {

    public HandledRuntimeException() {
        super();
    }

    public HandledRuntimeException(Throwable cause) {
        super(cause);
    }

    public HandledRuntimeException(String message) {
        super(message);
    }

    public HandledRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
