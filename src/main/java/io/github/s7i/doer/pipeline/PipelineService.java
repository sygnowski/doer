package io.github.s7i.doer.pipeline;

import io.github.s7i.doer.domain.grpc.GrpcServer;
import io.github.s7i.doer.pipeline.proto.PipelinePublishRequest;
import io.github.s7i.doer.pipeline.proto.PipelinePublishResponse;
import io.github.s7i.doer.pipeline.proto.PipelineServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "pipeline")
@Slf4j(topic = "doer.console")
public class PipelineService implements Runnable {

    @Option(names ="--port", defaultValue = "6565")
    private Integer port;

    public static void main(String[] args) {
        new CommandLine(PipelineService.class).execute(args);
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
        var service = new Handler();
        new GrpcServer(port, service)
                .startServer();
    }
}
