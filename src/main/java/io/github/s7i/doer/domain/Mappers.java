package io.github.s7i.doer.domain;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import io.github.s7i.doer.DoerException;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.pipeline.proto.PipelineLoad;
import io.github.s7i.doer.proto.Record;
import lombok.experimental.UtilityClass;

import java.util.HashMap;

import static java.util.Objects.nonNull;

@UtilityClass
public class Mappers {

    public static Output.Load mapFrom(PipelineLoad value) throws InvalidProtocolBufferException {
        if (!value.getLoad().is(Record.class)) {
            throw new DoerException("invalid load type: " + value.getLoad().getTypeUrl());
        }
        var rec = value.getLoad().unpack(Record.class);

        var bld = Output.Load.builder();

        if (rec.hasKey()) {
            bld.key(rec.getKey().getValue());
        }

        if (rec.hasResource()) {
            bld.resource(rec.getResource().getValue());
        }

        if (rec.hasData()) {
            var recordData = rec.getData().getValue();
            bld.data(recordData.toByteArray());
        }

        if (!rec.getMetaMap().isEmpty()) {
            bld.meta(new HashMap<>(rec.getMetaMap()));
        }

        return bld.build();

    }

    public static Record mapFrom(Output.Load load) {
        var b = Record.newBuilder();
        if (nonNull(load.getResource())) {
            b.setResource(StringValue.of(load.getResource()));
        }
        if (nonNull(load.getKey())) {
            b.setKey(StringValue.of(load.getKey()));
        }
        if (nonNull(load.getMeta())) {
            b.putAllMeta(load.getMeta());
        }
        var data = load.getData();
        if (nonNull(data) && data.length > 0) {
            b.setData(BytesValue.of(ByteString.copyFrom(data)));
        }
        return b.build();
    }

    public static Output.Load mapFrom(String resource, byte[] data) {
        return Output.Load.builder()
                .resource(resource)
                .data(data)
                .build();
    }
}
