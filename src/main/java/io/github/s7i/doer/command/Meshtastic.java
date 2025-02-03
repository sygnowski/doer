package io.github.s7i.doer.command;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.UnknownFieldSet;
import io.github.s7i.doer.ConsoleLog;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

@CommandLine.Command(name = "meshradio")
public class Meshtastic extends VerticleCommand {

    public static class Options {

        @Option(names = "-p", description = "port", defaultValue = "80")
        int port;
        @Option(names = "-h", description = "Radio IP", required = true)
        String host;
        @Option(names = "--interval", description = "Api call interval.", defaultValue = "5")
        int interval;
    }

    @Mixin
    private Options option;

    @Override
    protected Verticle createVerticle() {
        return new MeshWebClient(requireNonNull(option));
    }

    @RequiredArgsConstructor
    @Slf4j
    public static class MeshWebClient extends AbstractVerticle implements ConsoleLog {

        private final Options options;
        private WebClient client;

        @Override
        public void init(Vertx vertx, Context context) {
            super.init(vertx, context);

            var options = new WebClientOptions();
            client = WebClient.create(vertx, options);
        }

        @Override
        public void start() throws Exception {
            vertx.setPeriodic(TimeUnit.SECONDS.toMillis(options.interval), this::callRadio);
        }

        void callRadio(Long t) {
            var req = client.get(options.port, options.host, "/api/v1/fromradio?all=false");
            req.putHeader("Content-Type", "application/x-protobuf");
            req.send(rep -> {
                if (rep.succeeded()) {
                    if (rep.result().statusCode() == 200) {
                        var payload = rep.result().body().getBytes();
                        if (payload.length > 0) {
                            info("---\n{}\n---", Base64.getEncoder().encodeToString(payload));
                            try {
                                info("Proto: \n{}", UnknownFieldSet.parseFrom(payload));
                            } catch (InvalidProtocolBufferException bpe) {
                                log.warn("cannot parse proto: {}", payload);
                            }
                        }
                    }
                } else {
                    log.error("oops", rep.cause());
                }
            });
        }
    }
}
