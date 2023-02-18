package io.github.s7i.doer.command.dump

import io.github.s7i.doer.Globals
import io.github.s7i.doer.config.KafkaConfig
import io.github.s7i.doer.domain.kafka.KafkaConsumerFactory
import io.github.s7i.doer.domain.kafka.KafkaFactory
import io.github.s7i.doer.domain.kafka.KafkaProducerFactory
import io.github.s7i.doer.domain.output.OutputFactory
import io.github.s7i.doer.domain.output.OutputKind
import io.github.s7i.doer.domain.output.creator.OutputCreator
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.TopicPartition
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneOffset

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
            1 * createConsumer(_, _) >> consumer
        }
        def out = Mock(OutputCreator) {

        }
        def outputFactory = new OutputFactory().register(OutputKind.FILE, out)

        Globals.INSTANCE.kafka = new KafkaFactory(Mock(KafkaProducerFactory), consumerFactory)
        Globals.INSTANCE.getScope().outputFactory = outputFactory

        def dump = new KafkaDump()
        dump.yaml = new File("src/test/resources/simple-dump.yml")

        expect:
        dump.onExecuteCommand()
    }


    def "Dump Kafka to Kafka"() {
        setup:
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
            1 * createConsumer(_, _) >> consumer
        }

        def prodFactory = Mock(KafkaProducerFactory) {
            1 * createProducer(_, _) >> Mock(Producer) {
                10 * send(_)
            }
        }

        Globals.INSTANCE.kafka = new KafkaFactory(prodFactory, consumerFactory)


        def dump = new KafkaDump()
        dump.yaml = new File("src/test/resources/dump-to-kafka.yml")

        expect:
        dump.onExecuteCommand()
    }


    def "Dump from time"() {
        setup:
        def tp = new TopicPartition("fromTimeTopicName", 0)
        def ts = LocalDateTime.parse("2021-01-01T12:30:00").toInstant(ZoneOffset.UTC).toEpochMilli()
        def fromTimeMap = Map.of(tp, ts)

        def recordList = []
        10.times {
            def cr = new ConsumerRecord<>("fromTimeTopicName", 0, it, "key$it".toString(), "value$it".getBytes())
            recordList << cr
        }
        def records = Mock(ConsumerRecords) {
            count() >> recordList.size()
            forEach(_) >> { args ->
                recordList.forEach(args[0])
            }
        }

        def consumer = Mock(Consumer) {
            1 * committed(_) >> [:]
            1 * subscribe(["fromTimeTopicName"], _) >> { args ->
                (args[1] as ConsumerRebalanceListener).onPartitionsAssigned([tp])
            }
            1 * poll(_) >> records
            1 * offsetsForTimes(fromTimeMap)

        }
        def consumerFactory = Mock(KafkaConsumerFactory) {
            1 * createConsumer(_, _) >> consumer
        }

        Globals.INSTANCE.kafka = new KafkaFactory(Mock(KafkaProducerFactory), consumerFactory)

        def dump = new KafkaDump()
        dump.yaml = new File("src/test/resources/dump-from-time.yml")

        expect:
        dump.onExecuteCommand()
    }

    def "Dump Commit Offset Control"() {
        setup:
        def committedOffsets = []
        def kafkaConfig = null
        def tp = new TopicPartition("simple-one", 0)
        def callNo = 0;

        def recordList = []

        50.times {
            def cr = new ConsumerRecord<>("simple-one", 0, it, "key$it".toString(), "value$it".getBytes())
            recordList << cr
        }

        def consumer = Mock(Consumer) {
            1 * committed(_) >> [:]
            1 * subscribe(["simple-one"], _) >> { args ->
                (args[1] as ConsumerRebalanceListener).onPartitionsAssigned([tp])
            }
            5 * poll(_) >> { aPool ->
                //println("calling pool no#" + callNo)

                Mock(ConsumerRecords) {
                    def f = callNo++ * 10
                    def t = f + 10 -1
                    def recs = recordList[f..t]

                    count() >> recs.size()
                    forEach(_) >> { args ->
                        recs.forEach(args[0])
                    }
                }
            }

            5 * commitSync(_, _) >> { args ->
                committedOffsets << args[0]
            }
        }

        def consumerFactory = Mock(KafkaConsumerFactory) {
            1 * createConsumer(_, _) >> { args ->
                kafkaConfig = args[0]

                consumer
            }
        }

        Globals.INSTANCE.kafka = new KafkaFactory(Mock(KafkaProducerFactory), consumerFactory)

        def dump = new KafkaDump()
        dump.yaml = new File("src/test/resources/dump-with-offset-commit-control.yml")

        expect:
        dump.onExecuteCommand()

        def kafka = (kafkaConfig as KafkaConfig).getKafka()

        kafka.get(ConsumerConfig.MAX_POLL_RECORDS_CONFIG) == "10"
        kafka.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG) == "false"
        committedOffsets.stream()
                .map {(it[tp] as OffsetAndMetadata).offset()}
                .mapToLong {it}
                .collect() == [9, 19, 29, 39, 49]
    }
}
