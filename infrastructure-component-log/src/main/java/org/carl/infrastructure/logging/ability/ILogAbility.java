package org.carl.infrastructure.logging.ability;

import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;

/** normal */
public interface ILogAbility {

    default ILogger getLogger() {
        return LoggerFactory.getLogger(this.getClass());
    }
}
