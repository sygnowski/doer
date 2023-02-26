package io.github.s7i.doer.pipeline.grcp;

import com.google.protobuf.InvalidProtocolBufferException;
import io.github.s7i.doer.Doer;
import io.github.s7i.doer.domain.Mappers;
import io.github.s7i.doer.pipeline.BlockingPipe;
import io.github.s7i.doer.pipeline.PipePuller;
import io.github.s7i.doer.pipeline.proto.MetaOp;
import io.github.s7i.doer.pipeline.proto.PipelineLoad;
import io.grpc.stub.StreamObserver;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Slf4j
public class GrpcInboundConnection extends GrpcConnection {

    final BlockingPipe pipe = new BlockingPipe();
    @Setter
    Map<String, String> params = Collections.emptyMap();

    public GrpcInboundConnection(String target) {
        super(requireNonNull(target, "target"));
    }

    @Override
    public PipePuller lookupPuller() {
        return pipe;
    }

    @Override
    protected void onConnection() {
        super.onConnection();

        var meta = MetaOp.newBuilder()
                .setRequest(MetaOp.Request.newBuilder()
                        .setName(uuid)
                        .putAllParameters(params))
                .build();
        serviceStub.subscribe(meta, new StreamObserver<>() {
            @Override
            public void onNext(PipelineLoad value) {
                unload(value);
            }

            @Override
            public void onError(Throwable t) {
                log.error("oops", t);
            }

            @Override
            public void onCompleted() {
                log.debug("subscribe done");
            }
        });
    }

    private void unload(PipelineLoad value) {
        try {
            Doer.console().info("unpack {}", value);

            if (value.hasLoad()) {
                var load = Mappers.mapFrom(value);
                pipe.offer(load);
            }
        } catch (InvalidProtocolBufferException e) {
            log.error("oops", e);
        }
    }


    @Override
    public void close() throws Exception {

    }
}
