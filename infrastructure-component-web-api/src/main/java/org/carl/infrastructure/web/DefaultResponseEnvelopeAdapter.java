package org.carl.infrastructure.web;

public class DefaultResponseEnvelopeAdapter implements ResponseEnvelopeAdapter<ResponseEnvelope<?>> {

    @Override
    public ResponseEnvelope<?> success(Object data, WebRequestContext context) {
        return ResponseEnvelope.ok(data, context);
    }

    @Override
    public ResponseEnvelope<?> failure(WebError error, WebRequestContext context) {
        return ResponseEnvelope.failure(error, context);
    }
}
