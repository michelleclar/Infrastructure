package org.carl.infra.authorization;

import jakarta.annotation.Nonnull;

public interface IPermission {

    /**
     * @return permission name
     */
    String getName();

    /**
     * @return permission description
     */
    String getDescription();

    Boolean hasPermission(IUserIdentity identity, @Nonnull String requiredAction);
}
