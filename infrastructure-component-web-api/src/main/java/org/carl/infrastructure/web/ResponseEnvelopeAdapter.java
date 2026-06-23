package org.carl.infrastructure.web;

public interface ResponseEnvelopeAdapter<T> {

    T success(Object data, WebRequestContext context);

    T failure(WebError error, WebRequestContext context);
}
