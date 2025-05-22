package org.carl.infrastructure.tool.ability;

import org.jboss.logging.Logger;

public interface ILoggerAbility {
    Logger getLogger();

    default void handleSuccess(String format, Object... r) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debugf(format, r);
        }
    }
}
