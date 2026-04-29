package org.carl.infrastructure.persistence.engine.runtime;

import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;
import org.carl.infrastructure.persistence.sql.SqlLoggerListener;
import org.jooq.Configuration;
import org.jooq.ExecuteListenerProvider;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public interface JooqCustomContext {
    ILogger logger = LoggerFactory.getLogger(JooqCustomContext.class);

    /**
     * File "jooq-settings.xml"
     */
    default void apply(Configuration configuration) {
        Settings settings = configuration.settings();
        settings.setRenderCatalog(false);
        settings.setRenderSchema(false);
        settings.setExecuteLogging(SqlLoggerListener.sqlLog.isTraceEnabled()); // LoggerListener
        settings.setRenderFormatted(false);
        settings.setQueryTimeout(60); // seconds

        if (settings.isExecuteLogging()
                && configuration instanceof DefaultConfiguration defaultConfig) {
            configuration.executeListenerProviders();
            List<ExecuteListenerProvider> providers =
                    new ArrayList<>(Arrays.asList(configuration.executeListenerProviders()));
            providers.add(SqlLoggerListener::new);
            defaultConfig.setExecuteListenerProvider(
                    providers.toArray(new ExecuteListenerProvider[0]));

            Stream.of(configuration.executeListenerProviders())
                    .forEach(
                            p -> {
                                logger.debug("executeListenerProvider: {0}", p);
                            });
        }
    }
}
