package io.github.s7i.doer.config

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

    @Unroll
    def "expect pos #pos in range #exp"() {
        expect:
        def range = new Range(exp)
        in_range == range.in(pos)
        where:
        exp     | pos | in_range
        "1..10" | 1   | true
        "1..10" | 10  | true
        "1..10" | 11  | false
        "1..10" | 0   | false
        "..15"  | 17  | false
        "..15"  | 15  | true
        "..15"  | 0   | true
        "5.."   | 4   | false
        "5.."   | 5   | true
    }

    def "expect bad arg"() {
        when:
        def range = new Range("5..-5")
        then:
        thrown(IllegalArgumentException)

    }
}
