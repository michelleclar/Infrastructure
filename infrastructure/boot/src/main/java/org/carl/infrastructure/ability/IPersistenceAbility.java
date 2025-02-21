package org.carl.infrastructure.ability;

import org.carl.infrastructure.persistence.IPersistenceOperations;

public interface IPersistenceAbility {
    IPersistenceOperations getPersistenceOperations();
}
