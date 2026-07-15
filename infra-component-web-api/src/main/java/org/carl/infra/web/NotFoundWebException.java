package org.carl.infra.web;

import java.io.Serial;

public class NotFoundWebException extends WebApiException {

    @Serial private static final long serialVersionUID = 1L;

    public NotFoundWebException(String message) {
        super(WebError.notFound(message));
    }
}
