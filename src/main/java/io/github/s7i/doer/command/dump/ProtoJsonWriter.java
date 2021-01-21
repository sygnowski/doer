package io.github.s7i.doer.command.dump;

public interface ProtoJsonWriter {

    String toJson(String topic, byte[] data);
}
