package io.github.s7i.doer;

import static java.util.Objects.nonNull;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Range {

    private Long from;
    private Long to;

    public Range(String rangeExp) {
        var parts = rangeExp.split("\\..");
        if (parts.length == 1) {
            from = Long.valueOf(parts[0]);
        } else if (parts.length == 2) {
            if (!parts[0].isEmpty()) {
                from = Long.valueOf(parts[0]);
            }
            to = Long.valueOf(parts[1]);
        } else {
            throw new IllegalArgumentException("bad syntax");
        }
    }

    public boolean hasFrom() {
        return nonNull(from);
    }

    public boolean hasTo() {
        return nonNull(to);
    }
}
