package io.github.s7i.doer.command.dump

import io.github.s7i.doer.Globals
import io.github.s7i.doer.domain.kafka.KafkaConsumerFactory
import io.github.s7i.doer.domain.kafka.KafkaFactory
import io.github.s7i.doer.domain.kafka.KafkaProducerFactory
import io.github.s7i.doer.domain.output.OutputFactory
import io.github.s7i.doer.domain.output.OutputKind
import io.github.s7i.doer.domain.output.creator.OutputCreator
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import spock.lang.Specification

class KafkaDumpTest extends Specification {

    def "Dump Test"() {
        given:
        def recordList = []
        10.times {
            def cr = new ConsumerRecord<>("topicName", 0, it, "key$it".toString(), "value$it".getBytes())
            recordList << cr
        }
        def records = Mock(ConsumerRecords) {
            count() >> recordList.size()
            forEach(_) >> { args ->
                recordList.forEach(args[0])
            }
        }
        def consumer = Mock(Consumer) {
            1 * subscribe(["topicName"], _)
            1 * poll(_) >> records

        }
        def consumerFactory = Mock(KafkaConsumerFactory) {
            1 * createConsumer(_) >> consumer
        }
        def out = Mock(OutputCreator) {

        }
        def outputFactory = new OutputFactory().register(OutputKind.FILE, out)

        Globals.INSTANCE.kafka = new KafkaFactory(Mock(KafkaProducerFactory), consumerFactory)
        Globals.INSTANCE.outputFactory = outputFactory

        def dump = new KafkaDump()
        dump.yaml = new File("src/test/resources/simple-dump.yml")

        expect:
        dump.run()
    }
}
