package io.github.s7i.doer.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public abstract class Base {

    protected String version;

    @JsonProperty("type")
    @JsonAlias("kind")
    protected String kind;

    @JsonProperty("param")
    @JsonAlias({"parameters", "params"})
    protected Map<String, String> params = new HashMap<>();
}
