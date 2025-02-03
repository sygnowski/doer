package io.github.s7i.doer.command;

import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.ConsoleLog;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@CommandLine.Command(name = "meshradio")
public class Meshtastic extends VerticleCommand {

    @Getter
    public static class Options {

        @Option(names = "-p", description = "port", defaultValue = "80")
        int port;
        @Option(names = "-h", description = "Radio IP", required = true)
        String host;

    }

    @Mixin
    private Options option;

    @Override
    protected Verticle createVerticle() {
        return new MeshWebClient(requireNonNull(option));
    }
    @RequiredArgsConstructor
    @Slf4j
    public static class MeshWebClient extends AbstractVerticle implements ConsoleLog{

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
            vertx.setPeriodic(TimeUnit.SECONDS.toMillis(5), this::callRadio);
        }

        void callRadio(Long t) {
            var req = client.get(options.port, options.host, "/fromradio?all=false");
            req.putHeader("")
            req.send(rep -> {
                if (rep.succeeded()) {
                    if (rep.result().statusCode() == 200) {
                        var payload = rep.result().body().getBytes();
                        if (payload.length > 0) {
                            info(Base64.getEncoder().encodeToString(payload));
                        }
                    }
                }else {
                    log.error("oops", rep.cause());
                }
            });
        }
    }
}
