package io.github.s7i.doer

import spock.lang.Specification
import spock.lang.Unroll

class RangeTest extends Specification {

    @Unroll
    def "expect given range #exp"() {
        expect:
        def range = new Range(exp)
        from == range.getFrom()
        to == range.getTo()
        where:
        exp     | from | to
        "1..10" | 1    | 10
        "..15"  | null | 15
        "5.."   | 5    | null
    }
}
