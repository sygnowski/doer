package io.github.s7i.doer.manifest

import io.github.s7i.doer.util.Yamler

class ManifestTest extends spock.lang.Specification implements Yamler {


    def "dump multiple topics - cloning"() {
        setup:
        def dump = asDump("simple-dump-multiple-topic.yml")
        def mutation = dump.clone()
        mutation.getDump().getTopics().each {
            it.resolveName("cloned-" + it.getName())
        }
        expect:
        3.times {no ->
            assert "cloned-" + dump.getDump().getTopics()[no].getName() == mutation.getDump().getTopics()[no].getName()
        }

    }
}
