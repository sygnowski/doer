package io.github.s7i.doer.pipeline.store;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.*;

public class PipelineStorage<P> {

    public enum Direction {
        FIFO, LIFO;

        <T> Optional<Element<T>> from(Element<T> e) {
            requireNonNull(e, "element");
            switch (this) {
                case FIFO:
                    return e.getPrev();
                case LIFO:
                    return e.getNext();
            }
            throw new IllegalStateException();
        }
    }

    public interface OffsetSequence {
        Long nextOffset();
    }

    static class PipeIterator<P> implements Iterator<Element<P>> {

        Element<P> current;
        Element<P> initial;
        final Direction direction;

        PipeIterator(Direction direction, Element<P> initial) {
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
        public Element<P> next() {
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

    @Data
    @Accessors(fluent = true)
    public static class Pipe<P> {
        Element<P> tail;
        Element<P> head;

        public void putElementWithPackage(P pack, OffsetSequence oseq) {
            var link = new Link<P>();
            var el = new Element<P>(link, oseq.nextOffset(), pack);

            if (nonNull(tail)) {
                tail.link.prev = el;
                link.next = tail;
            }
            tail = el;

            if (isNull(head)) {
                head = el;
            }
        }
        public Iterable<Element<P>> iterator(Direction direction) {
            Supplier<Iterator<Element<P>>> prov  = () -> new PipeIterator<>(
                    direction,
                    direction == Direction.FIFO ? head : tail
            );

            return prov::get;
        }

        public Stream<Element<P>> stream(Direction direction) {
            return StreamSupport.stream(iterator(direction).spliterator(), false);
        }
    }

    @RequiredArgsConstructor
    @ToString
    public static class Element<P> {

        final Link<P> link;

        final Long offset;
        final P pack;

        public P getPackage() {
            return pack;
        }

        public Optional<Element<P>> getPrev() {
            return Optional.ofNullable(link.prev);
        }

        public Optional<Element<P>> getNext() {
            return Optional.ofNullable(link.next);
        }
    }


    static class Link<P> {
        Element<P> prev;
        Element<P> next;

        @Override
        public String toString() {
            return String.format("Link[ prev: %d, next: %d ]",
                    nonNull(prev) ? prev.offset : -1,
                    nonNull(next) ? next.offset : -1
            );
        }
    }

    private Map<String, Pipe<P>> store = new HashMap<>();
    private Long offsetSequence = 0L;

    private Long nextOffset() {
        return offsetSequence++;
    }

    public Pipe<P> addToPipe(String pipeName, Collection<P> packages) {
        var pipe = store.computeIfAbsent(pipeName, name -> new Pipe<>());
        for (var pack : packages) {
            pipe.putElementWithPackage(pack, this::nextOffset);
        }
        return pipe;
    }

    public Pipe<P> addToPipe(String pipeName, P pack) {
        return addToPipe(pipeName, List.of(pack));
    }

    public Optional<Pipe<P>> getPipe(String name) {
        return Optional.ofNullable(store.get(name));
    }
}
