package io.github.s7i.doer.command.dump

import spock.lang.Specification

class VariantParserTest extends Specification {

    def "Convert"() {
        setup:
        def con = new VariantParser()
        expect:
        def variant = con.convert(input)
        variant ? variant.getOptions() : null == expected
        where:
        input                   | expected
        "optA=1,2,3"            | ["optA": ["1", "2", "3"]]
        "optA=1,2,3|optB=4,5,6" | ["optA": ["1", "2", "3"], "optB": ["4", "5", "6"]]
        "optA"                  | null
        null                    | null
        ""                      | null

    }
}
