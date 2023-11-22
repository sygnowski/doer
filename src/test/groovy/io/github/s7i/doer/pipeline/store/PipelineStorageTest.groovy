package io.github.s7i.doer.pipeline.store

import spock.lang.Specification

import java.util.stream.Collectors

class PipelineStorageTest extends Specification {
    def "AddToPipe - one element"() {
        given:
        def ps = new PipelineStorage<String>()
        when:
        def pipe = ps.addToPipe("pipe-name", "pack-of-data")

        then:
        pipe

        pipe.head.offset == 0
        pipe.tail.getPackage() == "pack-of-data"
    }


    def "AddToPipe - 5 elements"() {
        given:
        def ps = new PipelineStorage<String>()
        def els = []
        5.times {
            els << "package-of-data-${it + 1}"
        }

        when:
        def pipe = ps.addToPipe("pipe-name", els)

        then:
        pipe

        pipe.tail.getPackage() == "package-of-data-5"
        pipe.head.getPackage() == "package-of-data-1"

        pipe.tail.offset == 4L
        pipe.head.offset == 0L
    }

    def "Pipe - iterate"() {
        given:
        def ps = new PipelineStorage<String>()
        def els = []
        5.times {
            els << "data-${it + 1}"
        }

        when:
        def pipe = ps.addToPipe("pipe-name", els)
        def collected = pipe.stream(direction)
                .map { it.getPackage() }
                .collect(Collectors.toList())

        then:
        collected == expected

        where:
        direction                      | expected
        PipelineStorage.Direction.FIFO | ["data-1", "data-2", "data-3", "data-4", "data-5"]
        PipelineStorage.Direction.LIFO | ["data-5", "data-4", "data-3", "data-2", "data-1"]
    }

}
