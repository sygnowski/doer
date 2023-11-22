package io.github.s7i.doer.pipeline

import io.github.s7i.doer.Globals
import io.github.s7i.doer.domain.ConfigProcessor
import spock.lang.Specification

import java.nio.file.Path

class PipelineTest extends Specification {

    def "should init pipeline"() {
        given:
        def params = [
                "doer.pipeline.backend"                 : "kafka",
                "doer.pipeline.backend.kafka.properties": "bootstrap.servers=kafka:9093\n"

        ]
        def pipeline = Mock(Pipeline) {
        }
        Globals.INSTANCE.pipeline = pipeline

        when:
        new ConfigProcessor(Path.of("src/test/resources/pipeline/pipeline-config.yml").toFile()).processConfig()

        then:
        1 * pipeline.init(params)

    }
}
