package io.github.s7i.doer.pipeline;

import io.github.s7i.doer.pipeline.proto.PipelinePublishRequest;
import io.github.s7i.doer.pipeline.proto.PipelinePublishResponse;
import io.github.s7i.doer.pipeline.proto.PipelineServiceGrpc;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "pipeline")
@Slf4j(topic = "doer.console")
public class PipelineService implements Runnable {

    @Option(names ="--port", defaultValue = "6565")
    private Integer port;

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
        var hsm = new HealthStatusManager();


        final var server = ServerBuilder.forPort(port)
                .addService(new Handler())
                .addService(ProtoReflectionService.newInstance())
                .addService(hsm.getHealthService())
                .build()
                .start();

        log.info("Server Started: {}", server);

        server.awaitTermination();

    }
}
