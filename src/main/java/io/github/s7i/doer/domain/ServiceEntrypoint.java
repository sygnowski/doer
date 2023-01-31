package io.github.s7i.doer.domain;

import com.google.protobuf.Empty;
import io.github.s7i.doer.proto.DoerServiceGrpc;
import io.github.s7i.doer.proto.Sone;
import io.github.s7i.doer.util.GitProps;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class ServiceEntrypoint extends DoerServiceGrpc.DoerServiceImplBase {

    static class Pulsar {
        ExecutorService es = Executors.newFixedThreadPool(1);
        List<Consumer<String>> receivers = new CopyOnWriteArrayList<>();

        void begin() {
            es.submit(this::pulse);

        }

        void end() {
            es.shutdown();
            receivers.clear();
        }

        void makePulse(Consumer<String> p) {
            final var now = Instant.now();

            p.accept(String.format("%d|%s", now.getEpochSecond() ,now));
        }

        void pulse() {
            while (!es.isShutdown()) {
                try {
                    receivers.forEach(this::makePulse);
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    log.error("oops", e);
                    Thread.currentThread().interrupt();
                }
            }
        }

        void add(Consumer<String> receiver) {
            receivers.add(receiver);
        }
    }

    @Override
    public void version(Empty request, StreamObserver<Sone> responseObserver) {
        responseObserver.onNext(sone(new GitProps().toString()));
        responseObserver.onCompleted();
    }


    @Override
    public void pulse(Empty request, StreamObserver<Sone> responseObserver) {
        super.pulse(request, responseObserver);
    }

    public Sone sone(String s) {
        return Sone.newBuilder().setS(s).build();
    }
}
