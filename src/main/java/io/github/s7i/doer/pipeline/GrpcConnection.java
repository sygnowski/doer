package io.github.s7i.doer.pipeline;

import com.google.protobuf.Any;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.pipeline.proto.*;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

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

    final String target;
    public void connect() {
        channel = Grpc.newChannelBuilder(
                requireNonNull(target, "target"),
                InsecureChannelCredentials.create()
        ).build();
        service = PipelineServiceGrpc.newFutureStub(channel);
    }

    @Override
    public void registerPuller(PipePuller puller) {
        try {
            connect();
            var meta = meteNewClient();
            uuid = service.exchangeMeta(meta).get(TIMEOUT, TimeUnit.SECONDS).getResponse().getStatus();

            start(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        var load = puller.onNextLoad();
                        sendLoad(load).ifPresentOrElse(r -> puller.onAccept(), () -> log.error("!!! BUG !!! can't happened"));
                    }
                } catch (Exception e) {
                    log.error("oops", e);
                }
                log.debug("pipeline puller: end of work");
            });

        } catch (RuntimeException | TimeoutException | InterruptedException | ExecutionException e) {
            log.error("oops", e);
        }
    }

    @NotNull
    private static MetaOp meteNewClient() {
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
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void close() throws Exception {
        if (nonNull(thread)) {
            thread.interrupt();
        }
        channel.shutdownNow().awaitTermination(30, TimeUnit.SECONDS);
    }
}
