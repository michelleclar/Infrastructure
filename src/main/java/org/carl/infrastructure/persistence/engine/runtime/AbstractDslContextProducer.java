package org.carl.infrastructure.persistence.engine.runtime;

import io.agroal.api.AgroalDataSource;
import jakarta.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;

/**
 * Produces DSLContext
 *
 * @author <a href="mailto:leo.tu.taipei@gmail.com">Leo Tu</a>
 */
public abstract class AbstractDslContextProducer {
    private static final Logger log = Logger.getLogger(AbstractDslContextProducer.class);

    public DSLContext createDslContext(
            String sqlDialect, AgroalDataSource dataSource, String customConfiguration) {
        Objects.requireNonNull(sqlDialect, "sqlDialect");
        Objects.requireNonNull(dataSource, "dataSource");

        if (customConfiguration == null || customConfiguration.isEmpty()) {
            return createDslContext(sqlDialect, dataSource, new JooqCustomContext() {});
        } else {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) cl = JooqCustomContext.class.getClassLoader();
            try {
                Class<?> clazz = cl.loadClass(customConfiguration);
                JooqCustomContext instance =
                        (JooqCustomContext) clazz.getDeclaredConstructor().newInstance();
                return createDslContext(sqlDialect, dataSource, instance);
            } catch (Exception e) {
                log.error(customConfiguration, e);
                throw new RuntimeException(e);
            }
        }
    }

    public DSLContext createDslContext(
            String sqlDialect, AgroalDataSource dataSource, JooqCustomContext customConfiguration) {
        Objects.requireNonNull(sqlDialect, "sqlDialect");
        Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(customConfiguration, "customConfiguration");
        return DslContextFactory.create(sqlDialect, dataSource, customConfiguration);
    }

    /** CDI: Ambiguous dependencies */
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Qualifier public @interface DslContextQualifier {
        String value();
    }
}
