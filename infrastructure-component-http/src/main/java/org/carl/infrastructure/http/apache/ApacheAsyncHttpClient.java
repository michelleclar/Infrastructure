package org.carl.infrastructure.http.apache;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;
import org.carl.infrastructure.http.HttpClient;
import org.carl.infrastructure.http.HttpClientException;
import org.carl.infrastructure.http.HttpClientOptions;
import org.carl.infrastructure.http.HttpCompletionContext;
import org.carl.infrastructure.http.HttpErrorContext;
import org.carl.infrastructure.http.HttpInterceptor;
import org.carl.infrastructure.http.HttpRequest;
import org.carl.infrastructure.http.HttpRequestContext;
import org.carl.infrastructure.http.HttpResponse;
import org.carl.infrastructure.http.HttpResponseContext;
import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ApacheAsyncHttpClient implements HttpClient {

    private static final ILogger LOGGER = LoggerFactory.getLogger(ApacheAsyncHttpClient.class);

    private final HttpClientOptions options;
    private final CloseableHttpAsyncClient client;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ApacheAsyncHttpClient(HttpClientOptions options) {
        this.options = Objects.requireNonNull(options, "options");
        PoolingAsyncClientConnectionManager connectionManager =
                PoolingAsyncClientConnectionManagerBuilder.create()
                        .setMaxConnTotal(options.getMaxConnections())
                        .setMaxConnPerRoute(options.getMaxConnections())
                        .build();
        this.client = HttpAsyncClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig(options.getResponseTimeout()))
                .build();
        this.client.start();
    }

    @Override
    public CompletionStage<HttpResponse> execute(HttpRequest request) {
        Objects.requireNonNull(request, "request");
        if (closed.get()) {
            CompletableFuture<HttpResponse> failed = new CompletableFuture<>();
            failed.completeExceptionally(new HttpClientException("HTTP client is closed"));
            return failed;
        }

        HttpRequestContext requestContext = new HttpRequestContext(request);
        return runBeforeRequest(requestContext)
                .thenCompose(ignored -> runBeforeSend(requestContext))
                .thenCompose(ignored -> send(request))
                .thenCompose(response -> handleSuccess(request, response))
                .exceptionallyCompose(error -> handleFailure(request, unwrap(error)));
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                client.close();
            } catch (Exception e) {
                throw new HttpClientException("Failed to close HTTP client", e);
            }
        }
    }

    private CompletionStage<Void> runBeforeRequest(HttpRequestContext context) {
        CompletionStage<Void> chain = CompletableFuture.completedFuture(null);
        for (HttpInterceptor interceptor : options.getInterceptors()) {
            chain = chain.thenCompose(ignored -> interceptor.beforeRequest(context));
        }
        return chain;
    }

    private CompletionStage<Void> runBeforeSend(HttpRequestContext context) {
        CompletionStage<Void> chain = CompletableFuture.completedFuture(null);
        for (HttpInterceptor interceptor : options.getInterceptors()) {
            chain = chain.thenCompose(ignored -> interceptor.beforeSend(context));
        }
        return chain;
    }

    private CompletionStage<SimpleHttpResponse> send(HttpRequest request) {
        CompletableFuture<SimpleHttpResponse> future = new CompletableFuture<>();
        SimpleHttpRequest apacheRequest = toApacheRequest(request);
        client.execute(apacheRequest, new FutureCallback<>() {
            @Override
            public void completed(SimpleHttpResponse result) {
                future.complete(result);
            }

            @Override
            public void failed(Exception ex) {
                future.completeExceptionally(new HttpClientException("HTTP request failed", ex));
            }

            @Override
            public void cancelled() {
                future.completeExceptionally(new HttpClientException("HTTP request was cancelled"));
            }
        });
        if (request.getTimeout() != null) {
            future.orTimeout(request.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
        }
        return future;
    }

    private CompletionStage<HttpResponse> handleSuccess(HttpRequest request, SimpleHttpResponse apacheResponse) {
        HttpResponse response = toResponse(apacheResponse);
        HttpResponseContext responseContext = new HttpResponseContext(request, response);

        CompletionStage<Void> chain = CompletableFuture.completedFuture(null);
        for (HttpInterceptor interceptor : options.getInterceptors()) {
            chain = chain.thenCompose(ignored -> interceptor.afterResponseHeaders(responseContext));
        }
        for (HttpInterceptor interceptor : options.getInterceptors()) {
            chain = chain.thenCompose(ignored -> interceptor.afterResponseBody(responseContext));
        }
        return chain.thenCompose(ignored -> afterCompletion(request, response, null))
                .thenApply(ignored -> response);
    }

    private CompletionStage<HttpResponse> handleFailure(HttpRequest request, Throwable error) {
        Throwable componentError = error instanceof HttpClientException
                ? error
                : new HttpClientException("HTTP request failed", error);
        HttpErrorContext errorContext = new HttpErrorContext(request, componentError);

        CompletionStage<Void> chain = CompletableFuture.completedFuture(null);
        for (HttpInterceptor interceptor : options.getInterceptors()) {
            chain = chain.thenCompose(ignored -> interceptor.onError(errorContext));
        }
        return chain
                .exceptionally(onErrorFailure -> {
                    LOGGER.warn("HTTP onError interceptor failed", unwrap(onErrorFailure));
                    return null;
                })
                .thenCompose(ignored -> afterCompletion(request, null, componentError))
                .handle((ignored, completionFailure) -> {
                    if (completionFailure != null) {
                        LOGGER.warn("HTTP afterCompletion interceptor failed", unwrap(completionFailure));
                    }
                    throw new CompletionException(componentError);
                });
    }

    private CompletionStage<Void> afterCompletion(HttpRequest request, HttpResponse response, Throwable error) {
        HttpCompletionContext completionContext = new HttpCompletionContext(request, response, error);
        CompletionStage<Void> chain = CompletableFuture.completedFuture(null);
        for (HttpInterceptor interceptor : options.getInterceptors()) {
            chain = chain.thenCompose(ignored -> interceptor.afterCompletion(completionContext));
        }
        return chain;
    }

    private SimpleHttpRequest toApacheRequest(HttpRequest request) {
        URI uri = withQueryParams(request);
        SimpleHttpRequest apacheRequest = new SimpleHttpRequest(request.getMethod(), uri);
        request.getHeaders().forEach((name, values) ->
                values.forEach(value -> apacheRequest.addHeader(name, value)));
        if (request.hasBody()) {
            ContentType contentType = ContentType.parse(request.getContentType());
            apacheRequest.setBody(request.getBody(), contentType);
        }
        if (request.getTimeout() != null) {
            apacheRequest.setConfig(requestConfig(request.getTimeout()));
        }
        return apacheRequest;
    }

    private URI withQueryParams(HttpRequest request) {
        if (request.getQueryParams().isEmpty()) {
            return request.getUri();
        }
        try {
            URIBuilder builder = new URIBuilder(request.getUri());
            request.getQueryParams().forEach((name, values) ->
                    values.forEach(value -> builder.addParameter(name, value)));
            return builder.build();
        } catch (URISyntaxException e) {
            throw new HttpClientException("Invalid request URI", e);
        }
    }

    private HttpResponse toResponse(SimpleHttpResponse response) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (Header header : response.getHeaders()) {
            headers.computeIfAbsent(header.getName(), ignored -> new ArrayList<>()).add(header.getValue());
        }
        return new HttpResponse(
                response.getCode(),
                response.getReasonPhrase(),
                headers,
                response.getBodyBytes());
    }

    private RequestConfig requestConfig(java.time.Duration responseTimeout) {
        return RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(options.getConnectTimeout().toMillis()))
                .setResponseTimeout(Timeout.ofMilliseconds(responseTimeout.toMillis()))
                .build();
    }

    private static Throwable unwrap(Throwable error) {
        if (error instanceof CompletionException && error.getCause() != null) {
            return error.getCause();
        }
        return error;
    }
}
