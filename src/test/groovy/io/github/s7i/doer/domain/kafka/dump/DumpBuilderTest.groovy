package io.github.s7i.doer.domain.kafka.dump

import io.github.s7i.doer.flow.Variant
import io.github.s7i.doer.util.Yamler
import spock.lang.Specification

import java.nio.file.Path

class DumpBuilderTest extends Specification implements Yamler {

    def "build with variants"() {
        setup:
        def variant = new Variant(["optA": ["1", "2", "3"]])
        def manifest = asDump("simple-dump-with-params.yml")
        def job = new DumpBuilder()
                .setWorkDir(Path.of("."))
                .setManifest(manifest)
                .setVariant(variant)
                .build()
        expect:
        job
        job.getTaskCount() == 3
    }
}
