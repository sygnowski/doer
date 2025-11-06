package io.github.s7i.doer;

public class DoerException extends RuntimeException {

    public DoerException() {
        super();
    }

    public DoerException(String message) {
        super(message);
    }

    public DoerException(String message, Throwable cause) {
        super(message, cause);
    }

    public DoerException(Throwable cause) {
        super(cause);
    }
}
