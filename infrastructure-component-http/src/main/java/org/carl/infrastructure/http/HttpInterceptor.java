package org.carl.infrastructure.http;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface HttpInterceptor {

    default CompletionStage<Void> beforeRequest(HttpRequestContext context) {
        return completed();
    }

    default CompletionStage<Void> beforeSend(HttpRequestContext context) {
        return completed();
    }

    default CompletionStage<Void> afterResponseHeaders(HttpResponseContext context) {
        return completed();
    }

    default CompletionStage<Void> afterResponseBody(HttpResponseContext context) {
        return completed();
    }

    default CompletionStage<Void> onError(HttpErrorContext context) {
        return completed();
    }

    default CompletionStage<Void> afterCompletion(HttpCompletionContext context) {
        return completed();
    }

    private static CompletionStage<Void> completed() {
        return CompletableFuture.completedFuture(null);
    }
}
