package org.carl.infrastructure.annotations;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PreventDuplicateValidator {
    String[] includeFieldKeys() default {};

    String[] optionalValues() default {};

    long expireTime() default 10_000L;
}
