package io.github.s7i.doer.pipeline.grcp;

import io.github.s7i.doer.DoerException;
import io.github.s7i.doer.pipeline.PipeConnection;
import io.github.s7i.doer.pipeline.proto.MetaOp;
import io.github.s7i.doer.pipeline.proto.PipelineServiceGrpc;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
@Slf4j
public abstract class GrpcConnection implements PipeConnection, AutoCloseable {

    public static final int TIMEOUT = 10;
    protected ManagedChannel channel;
    protected PipelineServiceGrpc.PipelineServiceFutureStub service;

    protected String uuid;

    final String target;

    public void connect() {
        channel = Grpc.newChannelBuilder(
                requireNonNull(target, "target"),
                InsecureChannelCredentials.create()
        ).build();
        service = PipelineServiceGrpc.newFutureStub(channel);

        int retryNo=0;
        final int ofRetries=10;

        boolean success = false;
        do {
            try {
                success = introduceNewConnection();

            } catch (RuntimeException | TimeoutException | InterruptedException | ExecutionException e) {
                log.warn("oops but still working... {}/{}", retryNo + 1, ofRetries, e);
                try {
                    int t = 1000 + (1000 * retryNo);
                    TimeUnit.MILLISECONDS.sleep(t);
                } catch (InterruptedException x) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } while (!success && retryNo++ < ofRetries );

        if (!success) {
            throw new DoerException("[PIPELINE GRPC] FAILURE");
        }
    }

    boolean introduceNewConnection() throws ExecutionException, InterruptedException, TimeoutException {
        uuid = service.exchangeMeta(metaNewConnection())
                .get(TIMEOUT, TimeUnit.SECONDS)
                .getResponse()
                .getStatus();
        log.info("pipeline client id: {}", uuid);
        return true;
    }


    static MetaOp metaNewConnection() {
        return MetaOp.newBuilder()
                .setRequest(MetaOp.Request.newBuilder().setName("add-new-pipeline-client"))
                .build();
    }

    @SneakyThrows
    public void closeSafe() {
        close();
    }
}
