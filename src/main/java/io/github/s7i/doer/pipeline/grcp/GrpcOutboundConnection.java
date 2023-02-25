package io.github.s7i.doer.pipeline.grcp;

import com.google.protobuf.Any;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.pipeline.PipePuller;
import io.github.s7i.doer.pipeline.proto.PipelineLoad;
import io.github.s7i.doer.pipeline.proto.PipelinePublishRequest;
import io.github.s7i.doer.pipeline.proto.PipelinePublishResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

@Slf4j
public class GrpcOutboundConnection extends GrpcConnection {

    public GrpcOutboundConnection(String target) {
        super(target);
    }

    private Thread thread;
    private transient boolean isClosed;

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
