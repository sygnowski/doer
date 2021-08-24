package io.github.s7i.doer.manifest.ingest;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.function.Function;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import lombok.Getter;

@Getter
public class ValueSet {

    public static final ValueSet EMPTY = new ValueSet() {
        @Override
        public boolean equals(Object obj) {
            return EMPTY == obj;
        }
    };

    String name;
    List<String> attributes;
    List<List<String>> values;
    Long repeat;

    public Stream<List<String>> stream() {
        requireNonNull(values, "ValueSet::values");
        if (nonNull(repeat) && repeat > 1) {
            var r = LongStream.range(0, repeat)
                  .mapToObj(step -> values.stream())
                  .flatMap(Function.identity());
            return r;

        }
        return values.stream();
    }
}
