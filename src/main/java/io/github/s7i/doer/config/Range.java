package io.github.s7i.doer.config;

import static java.util.Objects.isNull;
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
            if (nonNull(from) && to < from) {
                throw new IllegalArgumentException(String.format("to (%d) less than from (%d)", to, from));
            }
        } else {
            throw new IllegalArgumentException("bad syntax of range: " + rangeExp);
        }
    }

    public boolean hasFrom() {
        return nonNull(from);
    }

    public boolean hasTo() {
        return nonNull(to);
    }

    public boolean positionNotInRange(long pos) {
        return !positionInRange(pos);
    }

    public boolean positionInRange(long pos) {
        if (hasFrom() && pos < from) {
            return false;
        }
        if (hasTo() && pos > to) {
            return false;
        }
        return true;
    }

    public boolean reachEnd(long pos) {
        return isNull(to) ? false : pos >= to;
    }
}
