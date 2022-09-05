package io.github.s7i.doer.domain.output

import io.github.s7i.doer.pipeline.BlockingPipePusher
import io.github.s7i.doer.pipeline.PipeConnection
import spock.lang.Specification

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PipelineOutputTest extends Specification {


    def "Pipeline output test"() {
        given:
        def thrSrv = Executors.newFixedThreadPool(1)
        def load = Output.Load.builder()
                .data("test".getBytes())
                .build()
        BlockingPipePusher pusher
        def pipeConnection = Mock(PipeConnection) {
            registerPusher(_) >> { args ->
                pusher = args[0]
            }
        }
        def out = new PipelineOutput(pipeConnection)

        when:
        out.open()
        thrSrv.submit({ out.emit(load) })
        def nextLoad = pusher.onNextLoad()
        pusher.onAccept()

        then:
        thrSrv.shutdown()
        thrSrv.awaitTermination(1, TimeUnit.SECONDS)

        nextLoad == load
    }

}
