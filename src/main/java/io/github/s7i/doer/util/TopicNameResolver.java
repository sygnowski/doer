package io.github.s7i.doer.util;

import io.github.s7i.doer.Context;

public class TopicNameResolver implements Context {

    public TopicWithResolvableName resolve(TopicWithResolvableName topic) {
        String name = topic.getName();
        var resolved = getPropertyResolver().resolve(name);
        topic.resolveName(resolved);
        return topic;
    }

}
