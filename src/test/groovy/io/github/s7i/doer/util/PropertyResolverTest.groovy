package io.github.s7i.doer.util

import spock.lang.Specification

class PropertyResolverTest extends Specification {

    def "UUID property test"() {
        expect:
        def result = new PropertyResolver().resolve("\${__UUID}")
        result ==~ /\b[0-9a-f]{8}\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\b[0-9a-f]{12}\b/

    }


    def 'doer call test'() {
        setup:
        def pr = new PropertyResolver()
        def arg
        pr.setHandle({ callArg ->
            arg = callArg
            "it's working"
        })
        expect:
        pr.addProperty("val", "OK")
        pr.resolve('${doer://pipeline/${val}/1/2/3}') == "it's working"
        arg == "doer://pipeline/OK/1/2/3"
    }

    def 'test 123'() {
        expect:
        1 == 2
    }
}
