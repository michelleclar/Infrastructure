package org.carl.infrastructure.component.web.annotations;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.*;

/** NOTE: controller logged, this annotations can swallow exception */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Inherited
public @interface ControllerLogged {}
