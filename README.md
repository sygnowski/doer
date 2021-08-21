![Gradle Build](https://github.com/sygnowski/doer/workflows/Gradle%20Build/badge.svg) [![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/sygnowski/doer.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/sygnowski/doer/context:java)

# Doer

Small tool for doing big things.

### Commands:

List of available commands:

- kfeed - Kafka topics data populator
- kdump - Kafka topic consumer with rich options
- rocks - RocksDB support

### Working with Kafka

Samples of Doer - Kafka specific command usage

Collecting data by using `kdump` command with proto message content. The dump file manifest: `dump.yml`:

```yaml
# kafka-dump configuration file
version: "1.0"
type: "kafka-dump"
kafka-properties: kafka.properties
kafka:
  group.id: "kafka-dump001" # will override kafka properties 
dump:
  pool_timeout_sec: 5
  proto:
    descriptor_set:
      - "path/to/proto.desc"
  topics:
    - name: topicName
      range: 2000.. #from offset 2000 to infinity
      value: # specification for proto-value type
        proto_message: ProtoMessageName
      output: dump-dir/topicName
```

Run a command `doer kdump -y dump.yml` or `doer dump.yml`

Producing (Feeding) Kafka messages

A simple example of ingest manifest `simple-ingest.yml`

```yaml
version: "1.0"
kind: kafka-ingest
kafka:
  bootstrap.servers: localhost:9092
ingest:
  topics:
    - name: topicName
      entries:
        - key: some-key-value
          value: some-message-value
        - value: no-key-value
```

Run a command `doer kfeed -y simple-ingest.yml` or `doer ingest.yml`


An advanced example with proto usage

Ingest file manifest: `ingest-proto.yml` Processing a given manifest will cause production of 3 message with proto encoded content.

```yaml
version: "1.0"
kind: kafka-ingest
kafka-properties: kafka.properties #optional 
kafka: # will override kafka properties 
  bootstrap.servers: "localhost:9092"
  key.serializer: "org.apache.kafka.common.serialization.StringSerializer"
  value.serializer: "org.apache.kafka.common.serialization.ByteArraySerializer"
ingest:
  proto:
    descriptor_set:
      - "path/to/proto.desc"
  templates:
    - name: my-teamplate.tpl
      content: |+
        {
          "id": "${ID}",
          "fieldOfTypeMyProtoMessageType": {
            "@type": "type.googleapis.com/MyProtoMessageType",            
            "timestamp": "${TIMESTAMP}",    
            "user": {
              "name": "${USER_NAME}",
              "login": "${USER_LOGIN}"              
            }
          }        
        }
  value_sets:
    - name: set1
      attributes:
        - KEY
        - TIMESTAMP
        - ID
        - USER_NAME,
        - USER_LOGIN
      values:
        - [
            "kafka-key-${__#}",
            "${date:yyyy-MM-dd}T${date:HH:mm:ss}+00:00",
            "${__UUID}",
            "USER_NAME_${__#}",
            "USER_LOGIN_${__#}",
        ]
      repeat: 3 # will generate 3 value-set entries
  topics:
    - name: topicName
      label: labelName
      value_set: set1 # reference to a value set     
      entries:
        - key: ${KEY} # value_set['set1'].attribute['KEY']
          headers:
            - name: header-name
              value: header-value
          value_template:
            proto_message: MessageContentProtoMessageType
            name: my-template.tpl
```
### RocksDB

```bash
doer rocks -db ./rocksdb -a init -cf mycf
doer rocks -db ./rocksdb -a put -cf mycf -k k123 -v v123
doer rocks -db ./rocksdb -a get -cf mycf -k k123
```
