package io.github.s7i.doer.pipeline.grcp;

import io.github.s7i.doer.pipeline.BlockingPipe;
import io.github.s7i.doer.pipeline.PipePuller;
import io.github.s7i.doer.pipeline.proto.MetaOp;
import io.github.s7i.doer.pipeline.proto.PipelineLoad;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GrpcInboundConnection extends GrpcConnection {

    final BlockingPipe pipe = new BlockingPipe();

    public GrpcInboundConnection(String target) {
        super(target);
    }

    @Override
    public PipePuller lookupPuller() {
        return pipe;
    }

    @Override
    protected void onConnection() {
        super.onConnection();

        var meta = MetaOp.newBuilder().build();
        serviceStub.subscribe(meta, new StreamObserver<>() {
            @Override
            public void onNext(PipelineLoad value) {
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {

            }
        });
    }

    @Override
    public void close() throws Exception {

    }
}
