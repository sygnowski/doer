package io.github.s7i.doer.command;

import io.github.s7i.doer.DoerException;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthGrpc;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.TimeUnit;

@CommandLine.Command(name = "grpc-health")
@Slf4j(topic = "doer.console")
public class GrpcHealth extends Command {

    @CommandLine.Parameters()
    String target;
    @CommandLine.Option(names = {"-s", "--service"}, defaultValue = "", description = "package_names.ServiceName")
    String srvName;
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
            check(healthSrv, request);
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private void check(HealthGrpc.HealthBlockingStub healthSrv, HealthCheckRequest request) {
        final var result = healthSrv.check(request);
        final var status = result.getStatus();
        final var hs = status.name();
        log.info("Health status: {}", hs);
    }
}
