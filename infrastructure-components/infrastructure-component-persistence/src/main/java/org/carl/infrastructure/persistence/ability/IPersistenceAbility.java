package org.carl.infrastructure.persistence.ability;

import org.carl.infrastructure.persistence.IPersistenceOperations;

public interface IPersistenceAbility {
    IPersistenceOperations getPersistenceOperations();
}
