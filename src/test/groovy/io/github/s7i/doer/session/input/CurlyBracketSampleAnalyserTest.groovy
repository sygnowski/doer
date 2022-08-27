package io.github.s7i.doer.session.input

import spock.lang.Shared
import spock.lang.Specification

class CurlyBracketSampleAnalyserTest extends Specification {

    @Shared
    def analyzer = new CurlyBracketSampleAnalyser()

    def 'Analyze Samples'() {
        expect:
        is_completed == analyzer.processSample(an_input).isCompletedSample()
        where:
        an_input | is_completed
        "{.{"    | false
        "{."     | false
        ".}."    | false
        "}.}"    | true

    }

}
