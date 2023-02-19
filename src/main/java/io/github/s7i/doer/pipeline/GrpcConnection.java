package io.github.s7i.doer.pipeline;

import com.google.protobuf.Any;
import io.github.s7i.doer.DoerException;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.pipeline.proto.*;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
@Slf4j
public class GrpcConnection implements PipeConnection, AutoCloseable {

    public static final int TIMEOUT = 10;
    private ManagedChannel channel;
    private PipelineServiceGrpc.PipelineServiceFutureStub service;
    private String uuid;
    private Thread thread;

    private transient boolean isClosed;

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

    @Override
    public void registerPuller(PipePuller puller) {
        connect();

        start(() -> {
            try {
                while (!Thread.currentThread().isInterrupted() && !isClosed) {
                    var load = puller.onNextLoad();
                    if (nonNull(load)) {
                        sendLoad(load).ifPresentOrElse(
                                r -> puller.onAccept(),
                                () -> log.error("!!! BUG !!! can't happened")
                        );
                    }
                }
            } catch (Exception e) {
                log.error("oops", e);
            }
            log.debug("[PIPELINE GRPC] : pipeline puller - end of work");
        });
    }

    private static MetaOp metaNewConnection() {
        return MetaOp.newBuilder()
                .setRequest(MetaOp.Request.newBuilder().setName("add-new-pipeline-client"))
                .build();
    }

    Optional<PipelinePublishResponse> sendLoad(Output.Load load) {
        requireNonNull(load, "load");
        try {
            var request = PipelinePublishRequest.newBuilder()
                    .setPipelineLoad(PipelineLoad.newBuilder()
                            .putHeaders("client.uuid", uuid)
                            .setLoad(Any.pack(load.toRecord())))
                    .build();
            return Optional.ofNullable(service.publish(request).get(TIMEOUT, TimeUnit.SECONDS));
        } catch (RuntimeException | TimeoutException | InterruptedException | ExecutionException e) {
            log.error("oops", e);
        }
        return Optional.empty();
    }

    private void start(Runnable task) {
        thread = new Thread(task, "grpc-pipeline-puller");
        //thread.setDaemon(true);
        thread.start();
    }


    @SneakyThrows
    public void closeSafe() {
        close();
    }

    @Override
    public void close() throws Exception {
        log.debug("[PIPELINE GRPC] : closing");

        TimeUnit.MILLISECONDS.sleep(1000);

        isClosed = true;

        if (nonNull(thread)) {
            thread.interrupt();
        }
        channel.shutdownNow().awaitTermination(30, TimeUnit.SECONDS);
    }
}
