package io.github.s7i.doer.domain.grpc;

import io.grpc.BindableService;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = "doer.console")
@RequiredArgsConstructor
public class GrpcServer {

    private final HealthStatusManager hsm = new HealthStatusManager();
    private Server server;

    private final int port;
    private final BindableService service;

    public GrpcServer startServer() throws IOException {

        server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                .addService(service)
                .addService(ProtoReflectionService.newInstance())
                .addService(hsm.getHealthService())
                .build()
                .start();

        log.info("Server Started: {}", server);

        Runtime.getRuntime().addShutdownHook(new Thread(this::stopServer, "grpc-server-shutdown-hook"));

        try {
            server.awaitTermination();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("oops", e);
        }
        log.info("bye bye");

        return this;
    }

    @SneakyThrows
    private void stopServer() {
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
}
