package io.github.s7i.doer.command

import spock.lang.Specification

class SinkCommandTest extends Specification {


    def "parse manifest test"() {
        given:
        def manifestPath = "src/test/resources/pipeline/sink.yml"
        def sink = ManifestFileCommand.Builder.fromManifestFile(SinkCommand.class, new File(manifestPath))

        expect:
        sink.onExecuteCommand()
    }
}
