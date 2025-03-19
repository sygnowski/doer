package io.github.s7i.doer.command;

import io.github.s7i.doer.DoerException;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class VerticleCommand extends Command {

    protected abstract Verticle createVerticle();

    @Override
    public void onExecuteCommand() {
        try {
            var vertex = Vertx.vertx();
            var latch = new CountDownLatch(1);
            Runnable cleanup = () -> {
                log.info("running shutdown");
                vertex.close().onSuccess(e -> log.info("close success: {}", e));
                latch.countDown();
            };
            Runtime.getRuntime().addShutdownHook(new Thread(cleanup));

            vertex.deployVerticle(createVerticle(), ar -> {
                if (ar.succeeded()) {
                    log.info("deployed");
                } else {
                    log.error("oops", ar.cause());
                }
            });
            latch.await();
            log.info("end");
        } catch (Exception e) {
            throw new DoerException(e);
        }
    }
}
