package io.github.s7i.doer.command.serve;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

@Command(name = "serve")
@Slf4j
public class Serve implements Runnable {


    @Option(names = "port", defaultValue = "8010")
    Integer port;

    @Override
    public void run() {
        log.info("running server on port: {}", port);
        var srv = HttpServer.create()
              .route(r -> r.get("/", this::hello))
              .port(port)
              .bindNow();

        srv.onDispose()
              .block();

    }

    NettyOutbound hello(HttpServerRequest request, HttpServerResponse response) {
        return response.sendString(Mono.just("Doer serve for you..."));
    }

}
