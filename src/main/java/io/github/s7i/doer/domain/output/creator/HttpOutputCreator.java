package io.github.s7i.doer.domain.output.creator;

import io.github.s7i.doer.domain.output.HttpOutput;

public interface HttpOutputCreator extends OutputCreator {

    String getUri();

    default HttpOutput create() {
        return new HttpOutput(getUri());
    }
}
