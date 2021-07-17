package io.github.s7i.doer.pipeline;

import io.github.s7i.doer.pipeline.proto.PipelinePublishRequest;
import io.github.s7i.doer.pipeline.proto.PipelinePublishResponse;
import io.github.s7i.doer.pipeline.proto.PipelineServiceGrpc;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Command(name = "pipeline")
@Slf4j
public class PipelineService implements Runnable {

    public static void main(String[] args) {
        new PipelineService().run();
    }


    static class Handler extends PipelineServiceGrpc.PipelineServiceImplBase {
        @Override
        public void publish(PipelinePublishRequest request, StreamObserver<PipelinePublishResponse> responseObserver) {
            var pipelineLoad = request.getPipelineLoad();
            log.info("getting load: {}", pipelineLoad);


            responseObserver.onNext(PipelinePublishResponse.newBuilder()
                    .setStatus("ok")
                    .build());
            responseObserver.onCompleted();
        }
    }


    @SneakyThrows
    @Override
    public void run() {

        ServerBuilder.forPort(6565)
                .addService(new Handler())
                .build()
                .start()
                .awaitTermination();

    }
}
