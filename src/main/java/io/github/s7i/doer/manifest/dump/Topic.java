package io.github.s7i.doer.manifest.dump;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.s7i.doer.util.PropertyResolver;
import io.github.s7i.doer.util.TopicWithResolvableName;
import java.util.Optional;
import lombok.Getter;

@Getter
public class Topic implements TopicWithResolvableName {

    @JsonProperty("show_binary")
    boolean showBinary;
    String name;
    String range;
    String rule;
    Value value;
    String output;

    public Optional<String> getOutput() {
        if (nonNull(output)) {
            return Optional.of(new PropertyResolver().resolve(output));
        }
        return Optional.empty();
    }

    @Override
    public void resolveName(String name) {
        this.name = name;
    }
}
