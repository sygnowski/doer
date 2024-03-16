package io.github.s7i.doer.command

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.github.s7i.doer.Globals
import io.github.s7i.doer.domain.kafka.KafkaConsumerFactory
import io.github.s7i.doer.domain.kafka.KafkaFactory
import io.github.s7i.doer.domain.kafka.KafkaProducerFactory
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Future

class KafkaFeederTest extends Specification {

    def "simple message"() {
        given:
        def records = []
        def producer = Mock(Producer) {
            2 * send(_, _) >> { args ->
                records << args[0]
                Mock(Future)
            }
            1 * close()
        }
        def producerFactory = Mock(KafkaProducerFactory) {
            createProducer(_, _) >> producer
        }
        def feeder = new KafkaFeeder()
        Globals.INSTANCE.kafka = new KafkaFactory(producerFactory, Mock(KafkaConsumerFactory))
        feeder.yaml = new File("src/test/resources/simple-ingest.yml")
        expect:
        feeder.onExecuteCommand()
        records.any { it.value() == "no-key-value".getBytes() }
        records.any { it.value() == "some-message-value".getBytes() }

    }


    def "template"() {
        given:
        def records = []
        def producer = Mock(Producer) {
            1 * send(_, _) >> { args ->
                records << args[0]
                Mock(Future)
            }
            1 * close()
        }
        def producerFactory = Mock(KafkaProducerFactory) {
            createProducer(_, _) >> producer
        }
        def feeder = new KafkaFeeder()
        Globals.INSTANCE.kafka = new KafkaFactory(producerFactory, Mock(KafkaConsumerFactory))
        feeder.yaml = new File("src/test/resources/ingest-with-template.yml")
        expect:
        feeder.onExecuteCommand()
        def val = records.first().value()
        def valExpected = Files.readAllBytes(Path.of("src/test/resources/ingest-with-template-result.txt"))
        new String(val) == new String(valExpected)
    }

    def "template with value set - produce 2 records"() {
        given:
        def records = []
        def producer = Mock(Producer) {
            2 * send(_, _) >> { args ->
                records << args[0]
                Mock(Future)
            }
            1 * close()
        }
        def producerFactory = Mock(KafkaProducerFactory) {
            createProducer(_, _) >> producer
        }
        def feeder = new KafkaFeeder()
        Globals.INSTANCE.kafka = new KafkaFactory(producerFactory, Mock(KafkaConsumerFactory))
        feeder.yaml = new File("src/test/resources/ingest-with-template-value-set.yml")
        expect:
        feeder.onExecuteCommand()

        records.any { it.value() == '{"a": "value-A","b": "value-B","c": "value-C"}'.getBytes() }
        records.any { it.value() == '{"a": "value-D","b": "value-E","c": "value-F"}'.getBytes() }

    }

    def "template with value set - repeat 10"() {
        given:
        def records = []
        def producer = Mock(Producer) {
            10 * send(_, _) >> { args ->
                records << args[0]
                Callback cb = args[1]
                cb.onCompletion(new RecordMetadata(
                        new TopicPartition("t", 0),
                        0,
                        records.size(),
                        System.currentTimeMillis(),
                        0L,
                        10,
                        10
                ), null)
            }
            1 * close()
        }
        def producerFactory = Mock(KafkaProducerFactory) {
            createProducer(_, _) >> producer
        }
        def feeder = new KafkaFeeder()
        Globals.INSTANCE.kafka = new KafkaFactory(producerFactory, Mock(KafkaConsumerFactory))
        feeder.yaml = new File("src/test/resources/ingest-with-template-value-set-repeat.yml")
        expect:
        feeder.onExecuteCommand()
    }

    def "template with RANDOM value set"() {
        given:
        def gson = new Gson();
        def records = []
        def producer = Mock(Producer) {
            13 * send(_, _) >> { args ->
                records << args[0]
                Callback cb = args[1]
                cb.onCompletion(new RecordMetadata(
                        new TopicPartition("t", 0),
                        0,
                        records.size(),
                        System.currentTimeMillis(),
                        0L,
                        10,
                        10
                ), null)
            }
            1 * close()
        }
        def producerFactory = Mock(KafkaProducerFactory) {
            createProducer(_, _) >> producer
        }
        def feeder = new KafkaFeeder()
        Globals.INSTANCE.kafka = new KafkaFactory(producerFactory, Mock(KafkaConsumerFactory))
        feeder.yaml = new File("src/test/resources/ingest/template-with-random.yml")
        expect:
        feeder.onExecuteCommand()
        records.forEach { rec ->
            def str = new String(rec.value())
            def nbr =  gson.fromJson(str, JsonObject.class).get("temperature").getAsDouble()
            assert nbr > -31d
            assert nbr < 61d
        }
    }

}
