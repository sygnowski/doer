package io.github.s7i.doer.session;

import lombok.Getter;

@Getter
public class Arg {

    String name;
    String value;

    public Arg(String raw) {
        String[] parts = raw.split("=");
        name = parts[0];
        value = parts[1];
    }
}
