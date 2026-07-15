package org.carl.infra.http;

import java.util.concurrent.CompletionStage;

public interface HttpClient extends AutoCloseable {

    CompletionStage<HttpResponse> execute(HttpRequest request);

    @Override
    void close();
}
