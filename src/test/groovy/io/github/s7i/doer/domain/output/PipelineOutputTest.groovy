package io.github.s7i.doer.domain.output

import io.github.s7i.doer.pipeline.BlockingPipePusher
import io.github.s7i.doer.pipeline.PipeConnection
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
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

    def "Pipeline output test with backlog"() {
        given:
        def loadTransfer =[]
        def thrCount=0
        def thrSrv = Executors.newFixedThreadPool(10, { r->
            def t = new Thread(r , "pusher-${++thrCount}")
            t.setDaemon(true)
            return t
        })
        BlockingPipePusher pusher
        def pipeConnection = Mock(PipeConnection) {
            registerPusher(_) >> { args ->
                pusher = args[0]
            }
        }
        def out = new PipelineOutput(pipeConnection)

        when:
        out.open()

        def cdlPush = new CountDownLatch(10)

        10.times {
            def task = {
                println "Running task ${Thread.currentThread().getName()}"

                out.emit(Output.Load.builder()
                    .resource(Thread.currentThread().getName())
                    .data("test".getBytes())
                    .build())

                cdlPush.countDown()
            }
            thrSrv.submit(task)
            println "Task submitted (# ${it})"
        }

        def cdlWorkDone = new CountDownLatch(10)
        new Thread({

            10.times {
                def nextLoad = pusher.onNextLoad()
                pusher.onAccept()

                loadTransfer << nextLoad
                cdlWorkDone.countDown()

                println "pull done for ${nextLoad.getResource()}"

            }
        }, "puller").start()


        then:
        cdlPush.await(5, TimeUnit.SECONDS)
        cdlWorkDone.await(5, TimeUnit.SECONDS)

        loadTransfer.size() == 10

        thrSrv.shutdown()
        thrSrv.awaitTermination(5, TimeUnit.SECONDS)
    }

}
