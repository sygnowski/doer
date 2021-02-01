package io.github.s7i.doer.session

import spock.lang.Specification

class InteractiveSessionTest extends Specification {

    def "test interactive session"() {
        given:
        def cmd = ":set myprop=myval aaa=bbb"
        def storage = Mock(ParamStorage)
        when:
        def session = new InteractiveSession()
        session.setStorage(storage)
        session.processCommand(cmd)

        then:
        1 * storage.update("myprop", "myval")
        1 * storage.update("aaa", "bbb")
    }

    def "test interactive session - empty params"() {
        given:
        def cmd = ":set"
        def storage = Mock(ParamStorage)
        when:
        def session = new InteractiveSession()
        session.setStorage(storage)
        session.processCommand(cmd)

        then:
        0 * storage.update(_, _)
    }

}
