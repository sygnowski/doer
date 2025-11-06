package io.github.s7i.doer.domain.meshtastic;

import io.github.s7i.meshtastic.Proto;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.Base64;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiVerticle extends AbstractVerticle {

    public static final String HOST = "0.0.0.0";
    public static final String PORT = "7123";


    HttpServer server;
    Router router;


    @Override
    public void init(Vertx vertx, Context context) {

        server = vertx.createHttpServer();
        router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.post("/from-radio-to-json")
              .produces(MimeMapping.getMimeTypeForExtension("json"))
              .handler(ctx ->
                    Optional.ofNullable(
                                ctx.body().isEmpty() ? null : ctx.body()
                          )
                          .map(rb -> rb.asJsonObject().getString("base64", null))
                          .ifPresentOrElse(base64 -> {
                              try {
                                  byte[] data = Base64.getDecoder().decode(base64);
                                  String jsonTxt = Proto.INSTANCE.asJsonTextFromRadio(data);
                                  ctx.end(jsonTxt);
                              } catch (Exception e) {
                                  ctx.fail(400, e);
                              }
                          }, () -> ctx.fail(400))
              );
    }

    @Override
    public void start(Promise<Void> start) throws Exception {
        server.requestHandler(router)
              .listen(Integer.parseInt(PORT), HOST)
              .onSuccess(v -> {
                  log.info("Server Running: {}:{}", HOST, PORT);
                  start.complete();
              })
              .onFailure(start::fail);
    }

}
