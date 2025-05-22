package org.carl.infrastructure.component.web.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Objects;

@ConfigMapping(prefix = "scaffold")
public interface ScaffoldConfig extends IProfile {

    @WithDefault("1")
    String superUserId();

    @WithDefault("24")
    int sessionTimeoutHour();

    @WithDefault("true")
    boolean useSession();

    default boolean isSuperUser(String userID) {
        Objects.requireNonNull(userID, "test userID is null");
        return userID.equals(superUserId());
    }
}
