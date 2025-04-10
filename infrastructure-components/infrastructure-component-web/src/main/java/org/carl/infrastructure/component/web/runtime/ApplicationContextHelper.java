package org.carl.infrastructure.component.web.runtime;

import jakarta.enterprise.inject.spi.CDI;
import org.jboss.logging.Logger;

public class ApplicationContextHelper {
    private static final Logger log = Logger.getLogger(ApplicationContextHelper.class);

    public static <T> T getBean(Class<T> clazz) {
        T bean = null;
        try {
            bean = CDI.current().select(clazz).get();
        } catch (Exception e) {
            log.warnf("Could not get Bean for class %s", clazz.getSimpleName());
        }
        return bean;
    }
}
