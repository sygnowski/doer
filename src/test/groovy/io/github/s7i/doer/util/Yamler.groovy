package io.github.s7i.doer.util

import io.github.s7i.doer.command.YamlParser
import io.github.s7i.doer.manifest.dump.Dump

import java.nio.file.Path

trait Yamler {

    def asDump(String resName) {
        YamlParser yml = { Path.of("src/test/resources/", resName).toFile() }
        def manifest = yml.parseYaml(Dump.class)
        println(manifest)
        return manifest
    }

}