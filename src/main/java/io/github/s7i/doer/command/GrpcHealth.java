package io.github.s7i.doer.command;

import io.github.s7i.doer.DoerException;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.TimeUnit;

@CommandLine.Command(name = "grpc-health", description = "gRPC Health (io.grpc.HealthGrpc)")
@Slf4j(topic = "doer.console")
public class GrpcHealth extends Command {

    @CommandLine.Parameters(paramLabel = "target")
    String target;
    @CommandLine.Option(names = {"-s", "--service"}, defaultValue = "", description = "package_names.ServiceName")
    String srvName;

    @CommandLine.Option(names = {"-w"}, description = "Use HealthGrpc::watch method instead HealthGrpc::check.")
    boolean watch;

    @CommandLine.Option(names = {"-n"}, description = "Number of repeated checks.", defaultValue = "0")
    Integer noTry;

    @CommandLine.Option(names = {"-d"}, description = "Delay between check in seconds", defaultValue = "1")
    Integer delay;

    @Override
    public void onExecuteCommand() {
        try {
            grpcHealth();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DoerException(e);
        }
    }

    public void grpcHealth() throws InterruptedException {
        final var channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
        try {
            final var healthSrv = HealthGrpc.newBlockingStub(channel);

            final var request = HealthCheckRequest.newBuilder()
                    .setService(srvName)
                    .build();
            if (watch) {
                try {
                    healthSrv.watch(request).forEachRemaining(this::logStatus);
                } catch (StatusRuntimeException e) {
                    log.warn("grpc-watch", e);
                }
            } else {
                if (noTry == 0) {
                    check(healthSrv, request);
                } else if (noTry > 0) {
                    repeatChecks(healthSrv, request);
                }
            }
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private void repeatChecks(HealthGrpc.HealthBlockingStub healthSrv, HealthCheckRequest request) throws InterruptedException {
        int n = 0;
        do {
            if (n != 0) {
                TimeUnit.SECONDS.sleep(delay);
            }
            check(healthSrv, request);
        } while (++n < noTry);
    }

    private void check(HealthGrpc.HealthBlockingStub healthSrv, HealthCheckRequest request) {
        try {
            final var result = healthSrv.check(request);
            logStatus(result);
        } catch (StatusRuntimeException e) {
            log.warn("grpc-check", e);
        }
    }

    private void logStatus(HealthCheckResponse result) {
        final var status = result.getStatus();
        final var hs = status.name();
        log.info("Health status: {}", hs);
    }
}
