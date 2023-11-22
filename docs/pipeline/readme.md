# Pipelines

Sample of pipeline specification:

```yaml
# Pipeline manifest draft
version: v1
kind: pipeline
params:
  doer.pipeline.backend: kafka
  doer.pipeline.backend.kafka.properties: |+
    bootstrap.servers=kafka:9093
spec:
  - pipeline: a | b
  - name: a
    description: "a" as instance of source
    manifest-file: source.yml
  - name: b
    description: "b" as instance of sink
    manifest-file: sink.yml
---

version: v2
kind: pipeline
params:
  doer.pipeline.backend: kafka
  doer.pipeline.backend.kafka.properties: |+
    bootstrap.servers=kafka:9093
spec:
  pipeline:
    - flow: a | b
  elements:
    - name: a
      description: "a" as instance of source
      manifest-file: source.yml
    - name: b
      description: "b" as instance of sink
      manifest-file: sink.yml
```