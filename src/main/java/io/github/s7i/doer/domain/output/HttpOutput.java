package io.github.s7i.doer.domain.output;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import static java.util.Objects.nonNull;

@Slf4j
@RequiredArgsConstructor
public class HttpOutput implements Output {

    private HttpClient httpClient;
    final String uri;

    @Override
    public void open() {
        if (nonNull(httpClient)) {
            return;
        }
        httpClient = HttpClient.newBuilder()
              .connectTimeout(Duration.ofSeconds(15))
              .build();
        log.debug("http client opened");
    }

    @Override
    public void emit(Load load) {
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
                log.debug("http response, @uri: {}, @status: {}, @body: {}\n @payload{}", uri, statusCode, hnd.body(), payload);
            }
        } catch (IOException e) {
            log.error("", e);
        } catch (InterruptedException e) {
            log.warn("", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() throws Exception {
    }
}
