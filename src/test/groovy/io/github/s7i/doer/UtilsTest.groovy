package io.github.s7i.doer

import io.github.s7i.doer.domain.output.ConsoleOutput
import spock.lang.Specification

class UtilsTest extends Specification {

    def "URI test"() {
        expect:
        def uri = new URI(str)

        scheme == uri.getScheme()
        authority == uri.getAuthority()
        path == uri.getPath()
        host == uri.getHost()
        ssp == uri.getSchemeSpecificPart()

        where:
        str                    | scheme  | authority | path        | host      | ssp
        "doer://kafka/123/456" | "doer"  | "kafka"   | "/123/456"  | "kafka"   | "//kafka/123/456"
        "/test/123"            | null    | null      | "/test/123" | null      | "/test/123"
        "kafka:topic"          | "kafka" | null      | null        | null      | "topic"
        "kafka://topic"        | "kafka" | "topic"   | ""          | "topic"   | "//topic"
        ConsoleOutput.CONSOLE  | "doer"  | "console" | ""          | "console" | "//console"

    }

}
