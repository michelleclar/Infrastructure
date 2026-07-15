package org.carl.infra.logging.ability;

import org.carl.infra.logging.ILogger;
import org.carl.infra.logging.LoggerFactory;

/** normal */
public interface ILogAbility {

    default ILogger getLogger() {
        return LoggerFactory.getLogger(this.getClass());
    }
}
