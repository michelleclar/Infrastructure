package org.carl.infrastructure.component.web.annotations.interceptor;

import static org.carl.infrastructure.component.web.config.JSON.JSON;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.core.Response;

import org.carl.infrastructure.component.web.annotations.Logged;
import org.carl.infrastructure.component.web.config.ExceptionReason;
import org.carl.infrastructure.component.web.config.exception.BizException;
import org.carl.infrastructure.component.web.config.exception.SysException;
import org.jboss.logging.Logger;

@Priority(1)
@Interceptor
@Logged
public class LoggingInterceptor {

    private final Logger log = Logger.getLogger(LoggingInterceptor.class);

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
            logResponse(startTime, response);
        }
        return response;
    }

    private void logResponse(long startTime, Object response) {
        try {
            long endTime = System.currentTimeMillis();
            if (log.isDebugEnabled()) {
                log.debugf("Response:%s", JSON.toJSONString(response));
                log.debugf("Elapsed time: %d ms", endTime - startTime);
            }
        } catch (Exception e) {
            log.errorf(e, "Error logging response: %s", e.getMessage());
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
        return Response.status(9999)
                .entity(
                        new ExceptionReason() {
                            @Override
                            public String getReason() {
                                return e.getMessage();
                            }

                            @Override
                            public String getErrorType() {
                                return "UNKNOWN_ERROR";
                            }

                            @Override
                            public int getCode() {
                                return 9999;
                            }
                        });
    }

    private void logRequest(InvocationContext context) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debugf("Method: %s", context.getMethod().toString());
        Object[] args = context.getParameters();
        for (Object arg : args) {
            try {
                log.debugf("Request: %s", JSON.toJSONString(arg));
            } catch (Exception e) {
                log.errorf(e, "Error in arg: %s", arg);
            }
        }
    }
}
