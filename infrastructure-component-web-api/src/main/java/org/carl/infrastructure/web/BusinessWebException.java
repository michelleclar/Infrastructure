package org.carl.infrastructure.web;

import java.io.Serial;

public class BusinessWebException extends WebApiException {

    @Serial private static final long serialVersionUID = 1L;

    public BusinessWebException(String message) {
        super(WebError.business(message));
    }

    public BusinessWebException(int status, String code, String message) {
        super(WebError.business(status, code, message));
    }

    public BusinessWebException(WebError error) {
        super(error);
    }
}
