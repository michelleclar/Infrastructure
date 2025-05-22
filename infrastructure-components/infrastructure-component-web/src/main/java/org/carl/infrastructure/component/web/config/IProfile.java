package org.carl.infrastructure.component.web.config;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * runtime get profile
 */
public interface IProfile {
    default ProfileMode profile() {
        Config config = ConfigProvider.getConfig();
        String upperCase = config.getValue("quarkus.profile", String.class).toUpperCase();
        return ProfileMode.valueOf(upperCase);
    }

    default boolean isTestMode() {
        return profile().equals(ProfileMode.TEST);
    }

    default boolean isProdMode() {
        return profile().equals(ProfileMode.PROD);
    }

    default boolean isDevMode() {
        return profile().equals(ProfileMode.DEV);
    }
}
