package io.github.s7i.doer.util;

public interface TopicWithResolvableName {

    String getName();

    void resolveName(String name);
}
