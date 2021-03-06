package io.github.s7i.doer

import io.github.s7i.doer.util.PropertyResolver
import spock.lang.Specification

class UtilsTest extends Specification {

    def uuidTest() {
        expect:
        def result = new PropertyResolver().resolve("\${__UUID}")
        result ==~ /\b[0-9a-f]{8}\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\b[0-9a-f]{12}\b/

    }

}
