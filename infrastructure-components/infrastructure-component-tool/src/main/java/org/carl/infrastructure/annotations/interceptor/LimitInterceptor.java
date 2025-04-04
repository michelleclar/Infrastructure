//package org.carl.infrastructure.annotations.interceptor;
//
//import io.smallrye.mutiny.Uni;
//import io.vertx.core.http.HttpServerRequest;
//import jakarta.annotation.Priority;
//import jakarta.inject.Inject;
//import jakarta.interceptor.AroundInvoke;
//import jakarta.interceptor.Interceptor;
//import jakarta.interceptor.InvocationContext;
//import jakarta.ws.rs.core.Response;
//import org.carl.infrastructure.annotations.Limit;
//import org.carl.infrastructure.cache.CacheStd;
//import org.carl.infrastructure.config.StatusType;
//import org.jboss.logging.Logger;
//
//import java.util.Objects;
//
//@Priority(1000)
//@Limit
//@Interceptor
//public class LimitInterceptor extends CacheStd {
//    private final Logger log = Logger.getLogger(LimitInterceptor.class);
//    @Inject HttpServerRequest request;
//
//    @AroundInvoke
//    Object limitingInvocation(InvocationContext ctx) throws InterruptedException {
//        String host = request.remoteAddress().host();
//        if (!"127.0.0.1".equals(host)) {
//            Object ifPresent = getCacheContext().localCacheContext.get(host).await().indefinitely();
//
//            if (!Objects.isNull(ifPresent)) {
//                return Uni.createFrom().item(Response.status(StatusType.ERROR_DUPLICATE).build());
//            }
//            getCacheContext().localCacheContext.set(host, host).subscribe().wait();
//        }
//
//        try {
//            return ctx.proceed();
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//            return Uni.createFrom().item(Response.status(StatusType.ERROR_DUPLICATE).build());
//        }
//    }
//}
