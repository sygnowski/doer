package io.github.s7i.doer.util;

import io.github.s7i.doer.Context;

public class TopicNameResolver implements Context {

    private final PropertyResolver propertyResolver = new PropertyResolver(this);

    public TopicWithResolvableName resolve(TopicWithResolvableName topic) {
        String name = topic.getName();
        var resolved = propertyResolver.resolve(name);
        topic.resolveName(resolved);
        return topic;
    }

}
