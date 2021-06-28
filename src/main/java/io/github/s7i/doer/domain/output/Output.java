package io.github.s7i.doer.domain.output;

public interface Output extends AutoCloseable {


    void open();

    void emit(String resource, byte[] data);

}
