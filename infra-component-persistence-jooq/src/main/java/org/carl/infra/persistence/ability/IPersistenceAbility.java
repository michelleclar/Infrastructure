package org.carl.infra.persistence.ability;

import org.carl.infra.persistence.IPersistenceOperations;

public interface IPersistenceAbility {
    IPersistenceOperations getPersistenceOperations();
}
