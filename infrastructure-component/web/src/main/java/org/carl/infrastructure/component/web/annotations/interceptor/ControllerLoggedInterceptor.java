package org.carl.infrastructure.component.web.annotations.interceptor;

import static org.carl.infrastructure.component.web.config.JSON.JSON;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.core.Response;

import org.carl.infrastructure.component.web.annotations.ControllerLogged;
import org.carl.infrastructure.component.web.config.exception.BizException;
import org.carl.infrastructure.component.web.config.exception.ExceptionReason;
import org.carl.infrastructure.component.web.config.exception.SysException;
import org.jboss.logging.Logger;

/** bug: reactive can before method */
@Priority(1)
@Interceptor
@ControllerLogged
public class ControllerLoggedInterceptor {

    private static final Logger log = Logger.getLogger(ControllerLoggedInterceptor.class);

    @AroundInvoke
    Object logInvocation(InvocationContext context) {
        long startTime = System.currentTimeMillis();
        logRequest(context);
        Object response = null;
        try {
            response = context.proceed();

        } catch (Throwable e) {
            response = handleException(e);
        } finally {
            response = logResponse(startTime, response);
        }
        return response;
    }

    private Object logResponse(long startTime, Object response) {

        try {
            if (log.isDebugEnabled()) {
                if (response instanceof Uni<?> uni) {
                    return uni.invoke(
                            item -> {
                                long endTime = System.currentTimeMillis();
                                log.debugf("Elapsed time: %d ms", endTime - startTime);
                                log.debugf("Response:%s", JSON.toJsonStringX(item));
                            });
                }
                if (response instanceof Multi<?> multi) {
                    return multi.invoke(
                            item -> {
                                log.debugf("Response:%s", JSON.toJsonStringX(item));
                            });
                }
                log.debugf("Response:%s", JSON.toJsonString(response));
            }
            return response;
        } catch (Exception e) {
            log.errorf(e, "Error logging response: %s", e.getMessage());
            return response;
        }
    }

    private Object handleException(Throwable e) {
        if (e instanceof BizException biz) {
            log.warnf("Biz Exception: %s", biz.getMessage());
            if (log.isDebugEnabled()) {
                log.error(e.getMessage(), e);
            }
            return biz.toResponse();
        }
        if (e instanceof SysException sys) {
            log.errorf(sys, "SysException: %s", e.getMessage());
            return sys.toResponse();
        }
        log.errorf(e, "Unhandled exception: %s", e.getMessage());
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

        return Response.status(entity.getCode()).entity(entity);
    }

    private void logRequest(InvocationContext context) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debugf("Method: %s", context.getMethod().toString());
        Object[] args = context.getParameters();
        for (Object arg : args) {
            try {
                log.debugf("Request: %s", JSON.toJsonString(arg));
            } catch (Exception e) {
                log.errorf(e, "Error in arg: %s", arg);
            }
        }
    }
}
