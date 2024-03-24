[![Gradle Build](https://github.com/sygnowski/doer/workflows/Gradle%20Build/badge.svg)](https://github.com/sygnowski/doer/actions/workflows/gradle.yml)

# Doer
```
▓█████▄  ▒█████  ▓█████  ██▀███ 
▒██▀ ██▌▒██▒  ██▒▓█   ▀ ▓██ ▒ ██▒ 
░██   █▌▒██░  ██▒▒███   ▓██ ░▄█ ▒ 
░▓█▄   ▌▒██   ██░▒▓█  ▄ ▒██▀▀█▄ 
░▒████▓ ░ ████▓▒░░▒████▒░██▓ ▒██▒ 
https://github.com/sygnowski/doer 
 ▒▒▓  ▒ ░ ▒░▒░▒░ ░░ ▒░ ░░ ▒▓ ░▒▓░ 
 ░ ▒  ▒   ░ ▒ ▒░  ░ ░  ░  ░▒ ░ ▒░ 
 ░ ░  ░ ░ ░ ░ ▒     ░     ░░   ░ 
   ░        ░ ░     ░  ░   ░ 
 ░ 
Usage: doer [-v] [COMMAND]
let's do big things...
  -v, --version
Commands:
  kfeed
  kdump
  proto        Protocol buffers decoder/encoder.
  helix        Helix Toolkit.
  rocks
  grpc-health  gRPC Health (io.grpc.HealthGrpc)
  zoosrv       Local Zookeeper Server
  misc         Miscellaneous command set.
 ```

Small tool for doing big things.

## Main Goals
  - [x] low code, text based, rich text features like resolving expressions `kafka://dest/topic`
  - [ ] pipelines and records (WIP: #14)

## Commands:

List of available commands:

- `kfeed`

  Kafka topics ingestion.
  
  Features:
  - [x] yaml manifest configuration
  - [x] proto processor
  - [x] templates

- `kdump`
  
  Kafka topic consumer with rich options.
  
  Features:
  
  - [x] yaml manifest configuration
  - [x] proto processor
  - [x] offset / timestamp navigation
    - [x] from_time: `timestamp`
    - [ ] to_time: `timestamp`
    - [x] from offset: `range: 5..`
    - [x] to offset: `range: ..5`
    - [x] between: `renge: 5..30`
  - [x] mvel rule support: `rule: mvel expression`

- `rocks`

  RocksDB support, [more info](docs/rocksdb.md)

  Features:
  - [x] list column families
  - [x] get value of key
  - [x] set value of key
  - [x] create a new column family
  - [x] list entries (key:value) of column family

- `helix`

  Helix Toolkit, [more info](docs/samples/helix/readme.md)


## Usage
### Working with Kafka

 - Objective: Collect data from the Kafka topic, starting from offset `2000`, and decode from `Protobuf` into `json` format.
   Results store in the `directory`. 

Command: `doer manifest.yml` or `doer kdump -y manifest.yml`

Manifest:
```yaml
# kafka-dump manifest file
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

- Objective: Insert into Kafka topic records.

Command `doer manifest.yml` or `doer kfeed -y manifest.yml` 

Mainfest:
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

- Objective: Insert into Kafka topic multiple records in the Protobuf format.

Command `doer manifest.yml` or `doer kfeed -y manifest.yml`

Manifest:
```yaml
version: "1.0"
kind: kafka-ingest
kafka-properties: kafka.properties #optional 
kafka: # will override kafka properties 
  bootstrap.servers: "localhost:9092"
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
