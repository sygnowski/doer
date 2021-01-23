package io.github.s7i.doer.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public abstract class Base {

    String version;

    @JsonProperty("type")
    @JsonAlias("kind")
    String kind;
}
