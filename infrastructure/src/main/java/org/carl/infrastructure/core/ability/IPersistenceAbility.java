package org.carl.infrastructure.core.ability;

import org.carl.infrastructure.persistence.IPersistenceOperations;

public interface IPersistenceAbility {
    IPersistenceOperations getPersistenceOperations();
}
