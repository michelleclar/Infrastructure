package org.carl.infrastructure.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApacheAsyncHttpClientTest {

    private HttpServer server;
    private HttpClient client;

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void executesRequestAndReturnsBodyHeadersAndStatus() throws Exception {
        server = startServer(exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals("from-test", exchange.getRequestHeaders().getFirst("X-Request-Source"));
            assertEquals("/echo?name=carl", exchange.getRequestURI().toString());
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            respond(exchange, 200, "text/plain", "received:" + requestBody);
        });
        client = HttpClientFactory.create();

        HttpResponse response = client.execute(HttpRequest.post(url("/echo"))
                        .queryParam("name", "carl")
                        .header("X-Request-Source", "from-test")
                        .body("hello")
                        .build())
                .toCompletableFuture()
                .get();

        assertEquals(200, response.getStatusCode());
        assertEquals("received:hello", response.getBodyAsString());
        assertEquals("text/plain", response.getHeaders().get("Content-type").getFirst());
    }

    @Test
    void nonSuccessResponseReturnsNormally() throws Exception {
        server = startServer(exchange -> respond(exchange, 503, "text/plain", "try later"));
        client = HttpClientFactory.create();

        HttpResponse response = client.execute(HttpRequest.get(url("/unavailable")).build())
                .toCompletableFuture()
                .get();

        assertEquals(503, response.getStatusCode());
        assertEquals("try later", response.getBodyAsString());
    }

    @Test
    void interceptorOrderIsStableForSuccess() throws Exception {
        server = startServer(exchange -> respond(exchange, 200, "text/plain", "ok"));
        RecordingInterceptor interceptor = new RecordingInterceptor();
        client = HttpClientFactory.create(HttpClientOptions.builder()
                .addInterceptor(interceptor)
                .build());

        HttpResponse response = client.execute(HttpRequest.get(url("/ok")).build())
                .toCompletableFuture()
                .get();

        assertEquals(200, response.getStatusCode());
        assertEquals(List.of(
                "beforeRequest",
                "beforeSend",
                "afterResponseHeaders",
                "afterResponseBody",
                "afterCompletion"
        ), interceptor.events);
    }

    @Test
    void interceptorOrderIsStableForTransportError() {
        RecordingInterceptor interceptor = new RecordingInterceptor();
        client = HttpClientFactory.create(HttpClientOptions.builder()
                .addInterceptor(interceptor)
                .build());

        CompletionException exception = assertThrows(CompletionException.class, () ->
                client.execute(HttpRequest.get("http://127.0.0.1:1/not-open")
                                .timeout(Duration.ofMillis(200))
                                .build())
                        .toCompletableFuture()
                        .join());

        assertInstanceOf(HttpClientException.class, exception.getCause());
        assertEquals(List.of("beforeRequest", "beforeSend", "onError", "afterCompletion"), interceptor.events);
    }

    @Test
    void timeoutTriggersErrorLifecycle() throws Exception {
        server = startServer(exchange -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, "text/plain", "slow");
        });
        RecordingInterceptor interceptor = new RecordingInterceptor();
        client = HttpClientFactory.create(HttpClientOptions.builder()
                .addInterceptor(interceptor)
                .build());

        CompletionException exception = assertThrows(CompletionException.class, () ->
                client.execute(HttpRequest.get(url("/slow"))
                                .timeout(Duration.ofMillis(100))
                                .build())
                        .toCompletableFuture()
                        .join());

        assertInstanceOf(HttpClientException.class, exception.getCause());
        assertEquals(List.of("beforeRequest", "beforeSend", "onError", "afterCompletion"), interceptor.events);
    }

    @Test
    void closeCanBeCalledMoreThanOnce() {
        client = HttpClientFactory.create();

        client.close();
        client.close();

        CompletionException exception = assertThrows(CompletionException.class, () ->
                client.execute(HttpRequest.get("http://127.0.0.1:1/closed").build())
                        .toCompletableFuture()
                        .join());
        assertInstanceOf(HttpClientException.class, exception.getCause());
    }

    private HttpServer startServer(ExchangeHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/", exchange -> {
            try {
                handler.handle(exchange);
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        });
        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.start();
        return httpServer;
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body) {
        try (exchange) {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private static final class RecordingInterceptor implements HttpInterceptor {

        private final List<String> events = new ArrayList<>();

        @Override
        public CompletionStage<Void> beforeRequest(HttpRequestContext context) {
            events.add("beforeRequest");
            return HttpInterceptor.super.beforeRequest(context);
        }

        @Override
        public CompletionStage<Void> beforeSend(HttpRequestContext context) {
            events.add("beforeSend");
            return HttpInterceptor.super.beforeSend(context);
        }

        @Override
        public CompletionStage<Void> afterResponseHeaders(HttpResponseContext context) {
            events.add("afterResponseHeaders");
            return HttpInterceptor.super.afterResponseHeaders(context);
        }

        @Override
        public CompletionStage<Void> afterResponseBody(HttpResponseContext context) {
            events.add("afterResponseBody");
            return HttpInterceptor.super.afterResponseBody(context);
        }

        @Override
        public CompletionStage<Void> onError(HttpErrorContext context) {
            events.add("onError");
            return HttpInterceptor.super.onError(context);
        }

        @Override
        public CompletionStage<Void> afterCompletion(HttpCompletionContext context) {
            events.add("afterCompletion");
            return HttpInterceptor.super.afterCompletion(context);
        }
    }
}
