package io.github.s7i.doer.ingest.record

import io.github.s7i.doer.Context
import io.github.s7i.doer.command.YamlParser
import io.github.s7i.doer.domain.ingest.IngestProcessor
import io.github.s7i.doer.domain.output.Output
import io.github.s7i.doer.manifest.ingest.IngestRecordManifest
import io.github.s7i.doer.util.PropertyResolver
import spock.lang.Specification

import java.nio.file.Path

class IngestRecordTest extends Specification {

    def "Parse Manifest"() {

        given:
        YamlParser yml = { Path.of("src/test/resources/ingest/ingest-record.yml").toFile() }
        def manifest = yml.parseYaml(IngestRecordManifest.class)

        def records = []
        def context = Mock(Context) {
            getParams() >> ["doer.output": "mock-me"]
            getPropertyResolver() >> new PropertyResolver()
            buildOutput(_) >> Mock(Output) {
                3 * emit(_) >> {
                    records << it[0]
                }
            }
        }

        new IngestProcessor(context).process(manifest)

        expect:
        manifest.getKind() == 'ingest'
        records.size() == 3
        (records[2] as Output.Load).getKey() == "key-record 3"
    }
}
