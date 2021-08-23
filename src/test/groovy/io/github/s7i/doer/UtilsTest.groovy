package io.github.s7i.doer

import spock.lang.Specification
import spock.lang.Unroll

class UtilsTest extends Specification {

    @Unroll
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
        "doer://console"       | "doer"  | "console" | ""          | "console" | "//console"

    }

}
