package io.github.s7i.doer.command.serve;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import reactor.core.publisher.Flux;
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

    @Option(names = "-a", description = "Show Access Log")
    boolean accessLog;

    @Override
    public void run() {
        log.info("running server on port: {}", port);
        if (accessLog) {
            log.info("with access logs");
        }
        var srv = HttpServer.create()
//              .route(r -> r.get("/", this::hello)
//                    .get("/envs", this::envs))
              .handle(this::handle)
              .port(port)
              .accessLog(accessLog)
              .bindNow();

        srv.onDispose()
              .block();


    }

    NettyOutbound handle(HttpServerRequest request, HttpServerResponse response) {
        log.info("handle, request {}", request);
        return response.sendString(Mono.just("ok"));
    }

    NettyOutbound hello(HttpServerRequest request, HttpServerResponse response) {
        return response
              .header(CONTENT_TYPE, TEXT_PLAIN)
              .sendString(Mono.just("Doer serve for you..."));
    }

    NettyOutbound envs(HttpServerRequest request, HttpServerResponse response) {

        var envs = System.getenv()
              .entrySet()
              .stream();
        var flux = Flux.fromStream(envs);
        return response.send(flux.map(e -> toBytes(e.getKey(), e.getValue())));
    }

    ByteBuf toBytes(String key, String value) {
        var obj = key + "=" + value;
        return ByteBufAllocator.DEFAULT
              .buffer()
              .writeBytes(obj.getBytes(StandardCharsets.UTF_8));
    }

}
