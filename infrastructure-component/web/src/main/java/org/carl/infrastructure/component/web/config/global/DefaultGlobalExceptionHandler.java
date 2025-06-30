package org.carl.infrastructure.component.web.config.global;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.carl.infrastructure.component.web.config.exception.BizException;
import org.carl.infrastructure.component.web.config.exception.ExceptionReason;
import org.carl.infrastructure.component.web.config.exception.SysException;
import org.jboss.logging.Logger;

@Provider
public class DefaultGlobalExceptionHandler implements ExceptionMapper<Exception> {
    private final Logger LOGGER = Logger.getLogger(DefaultGlobalExceptionHandler.class);

    @Override
    @Produces(MediaType.APPLICATION_JSON)
    public Response toResponse(Exception e) {

        if (e instanceof SysException sysException) {
            LOGGER.warnf("Biz Exception: [%s]", sysException.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error(e.getMessage(), e);
            }
            return sysException.toResponse();
        }
        if (e instanceof BizException bizException) {
            LOGGER.warnf("Biz Exception: [%s]", bizException.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error(e.getMessage(), e);
            }
            return bizException.toResponse();
        }
        Response.Status responseStatus = Response.Status.INTERNAL_SERVER_ERROR;
        LOGGER.error("Unhandled exception.", e.getMessage(), e);
        LOGGER.error(e.getMessage(), e);
        ExceptionReason entity =
                new ExceptionReason() {
                    @Override
                    public String getReason() {
                        return e.getMessage();
                    }

                    @Override
                    public String getScenario() {
                        return "UNKNOWN_ERROR";
                    }

                    @Override
                    public String getErrorType() {
                        return "UNKNOWN";
                    }

                    @Override
                    public int getCode() {
                        return -999;
                    }
                };
        // NOTE: other return Internal Server Error
        return Response.status(responseStatus.getStatusCode(), e.getMessage())
                .entity(entity)
                .build();
    }
}
