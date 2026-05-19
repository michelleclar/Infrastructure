package org.carl.infrastructure.http;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HttpResponse {

    private final int statusCode;
    private final String reasonPhrase;
    private final Map<String, List<String>> headers;
    private final byte[] body;

    public HttpResponse(int statusCode, String reasonPhrase, Map<String, List<String>> headers, byte[] body) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
        this.headers = copyMultiMap(headers);
        this.body = body == null ? new byte[0] : body.clone();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body.clone();
    }

    public String getBodyAsString() {
        return getBodyAsString(StandardCharsets.UTF_8);
    }

    public String getBodyAsString(Charset charset) {
        return new String(body, charset);
    }

    private static Map<String, List<String>> copyMultiMap(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((key, values) -> copy.put(key, List.copyOf(values)));
        }
        return Map.copyOf(copy);
    }
}
