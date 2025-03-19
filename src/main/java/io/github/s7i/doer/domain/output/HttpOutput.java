package io.github.s7i.doer.domain.output;

import static java.util.Objects.nonNull;

import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.s7i.doer.Doer;
import io.github.s7i.doer.Globals;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class HttpOutput implements Output {

    private HttpClient httpClient;
    final String uri;
    RetryRegistry registry;

    @Override
    public void open() {
        if (nonNull(httpClient)) {
            return;
        }
        httpClient = HttpClient.newBuilder()
              .connectTimeout(Duration.ofSeconds(15))
              .build();
        log.debug("http client opened");

        var config = RetryConfig.<Boolean>custom()
              .retryOnResult(success -> !success)
              .failAfterMaxAttempts(true)
              .waitDuration(RetrySettings.get().waitDuration())
              .maxAttempts(RetrySettings.get().attempts())
              .build();
        registry = RetryRegistry.of(config);
    }

    @Override
    public void emit(Load load) {
        boolean success = registry.retry("http-output")
              .executeSupplier(() -> emitHttpRequest(load));

        if (!success) {
            if (Globals.INSTANCE.hasFlag(Doer.FLAG_FAIL_FAST)) {
                log.error("FAIL-FAST: cannot emit output: {}", load);
                System.exit(Doer.EC_ERROR);
            } else {
                log.warn("cannot emit output: {}", load);
            }
        }
    }

    private boolean emitHttpRequest(Load load) {
        String payload = load.dataAsString();
        var request = HttpRequest.newBuilder()
              .uri(URI.create(uri))
              .header("Content-Type", "application/json")
              .POST(BodyPublishers.ofString(payload))
              .build();
        try {
            var hnd = httpClient.send(request, BodyHandlers.ofString());
            int statusCode = hnd.statusCode();
            if (statusCode < 200 || statusCode > 206) {
                log.warn("Content not accepted: http response, @uri: {}, @status: {}, @body: {}\n @payload{}", uri, statusCode, hnd.body(), payload);
                return false;
            }
            return true;
        } catch (IOException e) {
            log.error("Http request failed", e);
        } catch (InterruptedException e) {
            log.warn("", e);
            Thread.currentThread().interrupt();
        }
        return false;
    }

    @Override
    public void close() throws Exception {
    }
}
