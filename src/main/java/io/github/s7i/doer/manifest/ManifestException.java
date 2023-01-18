package io.github.s7i.doer.manifest;

import io.github.s7i.doer.DoerException;

public class ManifestException extends DoerException {
    public ManifestException(String message) {
        super(message);
    }
    public ManifestException(Throwable cause) {
        super(cause);
    }
}
