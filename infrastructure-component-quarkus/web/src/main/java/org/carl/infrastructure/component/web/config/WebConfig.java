package org.carl.infrastructure.component.web.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Objects;

@ConfigMapping(prefix = "web")
public interface WebConfig extends IProfile {

    @WithDefault("1")
    String superUserId();

    @WithDefault("24")
    int sessionTimeoutHour();

    @WithDefault("true")
    boolean useSession();

    default boolean isSuperUser(String userId) {
        Objects.requireNonNull(userId, "test userID is null");
        return userId.equals(superUserId());
    }
}
