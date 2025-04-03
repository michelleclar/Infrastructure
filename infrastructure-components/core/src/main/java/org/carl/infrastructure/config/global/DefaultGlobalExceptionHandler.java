package org.carl.infrastructure.config.global;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.carl.infrastructure.config.exception.BaseException;
import org.jboss.logging.Logger;

@Provider
public class DefaultGlobalExceptionHandler implements ExceptionMapper<Exception> {
    private final Logger log = Logger.getLogger(DefaultGlobalExceptionHandler.class);

    @Override
    @Produces(MediaType.APPLICATION_JSON)
    public Response toResponse(Exception e) {
        if (e instanceof BaseException exception) {
            return exception.getErrorResponse();
        }
        Response.Status responseStatus = Response.Status.INTERNAL_SERVER_ERROR;
        log.error("Unhandled exception.", e.getMessage(), e);
        // NOTE: other return Internal Server Error
        return Response.status(responseStatus).entity(responseStatus.getReasonPhrase()).build();
    }
}
