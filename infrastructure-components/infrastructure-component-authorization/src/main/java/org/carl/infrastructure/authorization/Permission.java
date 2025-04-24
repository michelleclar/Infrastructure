package org.carl.infrastructure.authorization;

public interface Permission {

    /**
     * @return permission name
     */
    String getName();

    /**
     * @return permission description
     */
    String getDescription();
}
