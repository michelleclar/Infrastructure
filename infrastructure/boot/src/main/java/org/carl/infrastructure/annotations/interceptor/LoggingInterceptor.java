package org.carl.infrastructure.annotations.interceptor;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.carl.infrastructure.annotations.Logged;
import org.jboss.logging.Logger;

@Priority(2020)
@Interceptor
@Logged
public class LoggingInterceptor {

    private final Logger log = Logger.getLogger(LoggingInterceptor.class);

    @AroundInvoke
    Object logInvocation(InvocationContext context) throws Exception {
        log.debugf("Method:%s", context.getMethod().getName());
        log.debugf("Params:%s", context.getParameters());
        Object ret = context.proceed();
        log.debugf("Return:%s", ret);
        return ret;
    }
}
