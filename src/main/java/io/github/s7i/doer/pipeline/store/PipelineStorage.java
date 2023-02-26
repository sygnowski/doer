package io.github.s7i.doer.pipeline.store;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.stream.Stream;

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

        public <T> Stream<Element<T>> streamFrom(Element<T> e) {
            ElementIterator<T> ei = direction -> e;
            return ei.stream(this);
        }
    }

    public interface OffsetSequence {
        Long nextOffset();
    }


    @Data
    @Accessors(fluent = true)
    public static class Pipe<P> implements ElementIterator<P> {
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

        @Override
        public Element<P> getStartElement(Direction direction) {
            return direction == Direction.FIFO ? head : tail;
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
