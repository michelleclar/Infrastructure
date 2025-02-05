package org.carl.infrastructure.persistence;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.carl.infrastructure.persistence.database.core.PersistenceProvider;
import org.jboss.logging.Logger;
import org.jooq.Configuration;

@Singleton
@IfBuildProperty(name = "quarkus.plugins.persistence.enable", stringValue = "true")
public class PersistenceService extends PersistenceProvider implements IPersistenceOperations {
    private static final Logger log = Logger.getLogger(PersistenceService.class);

    static Map<String, Object> dtoFactory = new HashMap<>();

    @Deprecated
    @SuppressWarnings("unchecked")
    public <T> T dto(Function<Configuration, T> function) {
        Configuration conf = dsl().configuration();
        String key = function.getClass().getSimpleName().split("[$]")[0];
        Object o = dtoFactory.get(key);
        if (o == null) {
            dtoFactory.put(key, function.apply(conf));
        }

        return (T) dtoFactory.get(key);
    }
}
