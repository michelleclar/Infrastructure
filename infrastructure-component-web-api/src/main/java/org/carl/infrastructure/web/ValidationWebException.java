package org.carl.infrastructure.web;

import java.io.Serial;
import java.util.List;

public class ValidationWebException extends WebApiException {

    @Serial private static final long serialVersionUID = 1L;

    public ValidationWebException(List<ValidationError> errors) {
        super(WebError.validation(errors));
    }
}
