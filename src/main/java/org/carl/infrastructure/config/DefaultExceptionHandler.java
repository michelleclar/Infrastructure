package org.carl.infrastructure.config;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class DefaultExceptionHandler implements ExceptionMapper<Exception> {
    private final Logger log = Logger.getLogger(DefaultExceptionHandler.class);

    @Override
    @Produces(MediaType.APPLICATION_JSON)
    public Response toResponse(Exception e) {

        log.error("Unhandled exception.", e.getMessage(), e);
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(e.getMessage())
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}
