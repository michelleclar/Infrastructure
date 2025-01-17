package org.carl.infrastructure.annotations.interceptor;

import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.carl.infrastructure.annotations.PreventDuplicateValidator;
import org.carl.infrastructure.cache.RemoteCacheService;
import org.carl.infrastructure.comment.DuplicationException;
import org.carl.infrastructure.comment.StatusType;
import org.carl.infrastructure.comment.Utils;
import org.jboss.logging.Logger;

@Interceptor
@PreventDuplicateValidator
@Priority(1000)
public class DuplicateInterceptor {
    private final Logger log = Logger.getLogger(DuplicateInterceptor.class);
    @Inject HttpServerRequest request;

    // TODO: need remote cache
    @Inject RemoteCacheService remoteCacheService;

    @AroundInvoke
    Object duplicateInterceptor(InvocationContext ctx) throws Exception {
        PreventDuplicateValidator interceptorBinding =
                ctx.getInterceptorBinding(PreventDuplicateValidator.class);
        var includeKeys = interceptorBinding.includeFieldKeys();
        var optionalValues = interceptorBinding.optionalValues();
        var expiredTime = interceptorBinding.expireTime();

        if (includeKeys == null || includeKeys.length == 0) {
            log.warn(
                    "[PreventDuplicateRequestAspect] ignore because includeKeys not found in"
                            + " annotation");
            return ctx.proceed();
        }

        var requestBody = request.body().result();
        if (requestBody == null) {
            log.warn(
                    "[PreventDuplicateRequestAspect] ignore because request body object find not"
                            + " found in method arguments");
            return ctx.proceed();
        }
        var requestBodyMap = requestBody.toJsonObject().getMap();
        var keyRedis = buildKeyRedisByIncludeKeys(includeKeys, optionalValues, requestBodyMap);
        var keyRedisMD5 = Utils.hashMD5(keyRedis);
        log.infof(
                "[PreventDuplicateRequestAspect] rawKey: [%s] and generated keyRedisMD5: [%s]",
                keyRedis, keyRedisMD5);
        deduplicateRequestByRedisKey(keyRedisMD5, expiredTime);
        return ctx.proceed();
    }

    private String buildKeyRedisByIncludeKeys(
            String[] includeKeys, String[] optionalValues, Map<String, Object> requestBodyMap) {

        var keyWithIncludeKey =
                Arrays.stream(includeKeys)
                        .map(requestBodyMap::get)
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .collect(Collectors.joining(":"));

        if (optionalValues.length > 0) {
            return keyWithIncludeKey + ":" + String.join(":", optionalValues);
        }
        return keyWithIncludeKey;
    }

    public void deduplicateRequestByRedisKey(String key, long expiredTime) {

        if (remoteCacheService.setExpireAfterWrite(key, expiredTime, key)) {
            log.info(
                    String.format(
                            "[PreventDuplicateRequestAspect] key: %s has set successfully !!!",
                            key));
            return;
        }

        log.warn(
                String.format(
                        "[PreventDuplicateRequestAspect] key: %s has already existed !!!", key));
        throw new DuplicationException(StatusType.ERROR_DUPLICATE);
    }
}
