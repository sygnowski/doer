package io.github.s7i.doer.ingest.record

import io.github.s7i.doer.Globals
import io.github.s7i.doer.command.YamlParser
import io.github.s7i.doer.domain.ingest.IngestProcessor
import io.github.s7i.doer.manifest.ingest.IngestRecordManifest
import spock.lang.Specification

import java.nio.file.Path

class IngestRecordTest extends Specification {

    def "Parse Manifest" () {

        given:
        YamlParser yml = { Path.of("src/test/resources/ingest/ingest-record.yml").toFile() }
        def manifest = yml.parseYaml(IngestRecordManifest.class)

        new IngestProcessor(Globals.INSTANCE).process(manifest)

        expect:
        manifest.getKind() == 'ingest'

    }

}
