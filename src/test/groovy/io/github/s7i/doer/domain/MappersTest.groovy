package io.github.s7i.doer.domain

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.google.protobuf.BytesValue
import com.google.protobuf.StringValue
import io.github.s7i.doer.pipeline.proto.PipelineLoad
import io.github.s7i.doer.proto.Record
import spock.lang.Specification

class MappersTest extends Specification {
    def "MapFrom PipelineLoad into Load"() {

        given:
        def pipelineLoad = PipelineLoad.newBuilder()
                .setLoad(Any.pack(Record.newBuilder()
                        .setData(BytesValue.of(ByteString.copyFromUtf8("this-is-my-string")))
                        .setKey(StringValue.of("key-123"))
                        .setResource(StringValue.of("my-res"))
                        .putMeta("meta-key", "meta-value")
                        .build()))
                .build()
        when:
        def load = Mappers.mapFrom(pipelineLoad)
        then:
        load.dataAsString() == "this-is-my-string"
        load.getKey() == "key-123"
        load.getResource() == "my-res"
        load.getMeta().get("meta-key") == "meta-value"
    }
}
