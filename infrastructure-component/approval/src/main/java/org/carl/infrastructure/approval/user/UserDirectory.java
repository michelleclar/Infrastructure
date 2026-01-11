package org.carl.infrastructure.approval.user;

import java.util.HashMap;
import java.util.Map;

public final class UserDirectory {
    private static final Map<String, String> USERS = new HashMap<>();

    static {
        USERS.put("carl", "Carl");
        USERS.put("jack", "Jack");
    }

    private UserDirectory() {}

    public static boolean exists(String userId) {
        return USERS.containsKey(userId);
    }

    public static String displayName(String userId) {
        return USERS.getOrDefault(userId, userId);
    }
}
