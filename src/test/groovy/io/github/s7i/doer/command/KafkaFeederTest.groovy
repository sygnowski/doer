package io.github.s7i.doer.command

import io.github.s7i.doer.kafka.KafkaProducerFactory
import org.apache.kafka.clients.producer.Producer
import spock.lang.Specification

import java.util.concurrent.Future

class KafkaFeederTest extends Specification {

    def "simple message"() {
        given:
        def records = []
        def producer = Mock(Producer) {
            2 * send(_) >> { args ->
                records << args[0]
                Mock(Future)
            }
            1 * close()
        }
        def kafkaFactory = Mock(KafkaProducerFactory) {
            build(_, _) >> producer
        }
        def feeder = new KafkaFeeder()
        feeder.kafkaProducerFactory = kafkaFactory
        feeder.yaml = new File("src/test/resources/simple-ingest.yml")
        expect:
        feeder.run()
        records.any { it.value() == "no-key-value".getBytes() }
        records.any { it.value() == "some-message-value".getBytes() }

    }

}
