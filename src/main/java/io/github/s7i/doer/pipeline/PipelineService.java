package io.github.s7i.doer.pipeline;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.StringValue;
import io.github.s7i.doer.domain.grpc.GrpcServer;
import io.github.s7i.doer.pipeline.proto.*;
import io.github.s7i.doer.proto.Record;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.nonNull;

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

        @Override
        public void exchangeMeta(MetaOp request, StreamObserver<MetaOp> responseObserver) {

            var response = MetaOp.newBuilder();

            if (request.getRequest().getName().equals("add-new-pipeline-client")) {
                log.info("new client");
                response.setResponse(MetaOp.Response.newBuilder().setStatus(UUID.randomUUID().toString()));
            }

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }
    }

    /**
     * <pre>
     * # proto-doer/src/main/proto/doer.proto:14
     *
     * message Record {
     *   google.protobuf.StringValue resource = 1;
     *   google.protobuf.StringValue key = 2;
     *   map<string, string> meta = 3;
     *   google.protobuf.BytesValue data = 4;
     * }
     * </pre>
     */
    @Command(name = "record", description = "Emit protobuf Record entity.")
    public void record(
            @Option(names = "--resource")
            String resource,
            @Option(names = "--key")
            String key,
            @Option(names = "--meta")
            Map<String, String> meta,
            @Option(names = "--data")
            String data,
            @Option(names = {"-pl", "--pipeline-load"}, description = "Embed Record in PipelineLoad::Load.")
            boolean pipelineLoad,
            @Option(names= {"-h", "--help"}, usageHelp = true)
            boolean help) {

        var b = Record.newBuilder();

        if (nonNull(resource)) {
            b.setResource(StringValue.of(resource));
        }

        if (nonNull(key)) {
            b.setKey(StringValue.of(key));
        }

        if (nonNull(meta)) {
            b.putAllMeta(meta);
        }

        if (nonNull(data)) {
            b.setData(BytesValue.of(ByteString.copyFromUtf8(data)));
        }

        try {
            byte[] bytes = pipelineLoad
                    ? PipelineLoad.newBuilder()
                        .setLoad(Any.pack(b.build()))
                        .build()
                        .toByteArray()
                    : b.build().toByteArray();

            System.out.write(bytes);
        } catch (IOException e) {
            log.error("new record", e);
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
