package io.github.s7i.doer.pipeline.store;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.*;

public interface ElementIterator<P> {

    class PipeIterator<P> implements Iterator<PipelineStorage.Element<P>> {

        PipelineStorage.Element<P> current;
        PipelineStorage.Element<P> initial;
        final PipelineStorage.Direction direction;

        public PipeIterator(PipelineStorage.Direction direction, PipelineStorage.Element<P> initial) {
            requireNonNull(initial, "initial");
            requireNonNull(direction, "direction");

            this.direction = direction;
            this.initial = initial;
        }

        @Override
        public boolean hasNext() {
            if (nonNull(initial)) {
                return true;
            }

            return nonNull(current) && direction.from(current).isPresent();
        }

        @Override
        public PipelineStorage.Element<P> next() {
            if (nonNull(initial)) {

                current = initial;
                initial = null;

                return current;
            }

            var nextStep = direction.from(requireNonNull(current,
                    "missing iterator element"
            ));
            current = nextStep.get();
            return current;
        }
    }

    PipelineStorage.Element<P> getStartElement(PipelineStorage.Direction direction);

    default Iterable<PipelineStorage.Element<P>> iteratorOf(PipelineStorage.Direction direction, PipelineStorage.Element<P> start) {

        if (isNull(start)) {
            return Collections::emptyIterator;
        }

        Supplier<Iterator<PipelineStorage.Element<P>>> prov  = () ->
                new PipeIterator<>(direction, start);

        return prov::get;
    }

    default Stream<PipelineStorage.Element<P>> stream(PipelineStorage.Direction direction) {
        var i = iteratorOf(direction, getStartElement(direction));
        return StreamSupport.stream(i.spliterator(), false);
    }
}
