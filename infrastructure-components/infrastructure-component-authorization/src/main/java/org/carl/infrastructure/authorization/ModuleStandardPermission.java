package org.carl.infrastructure.authorization;

/** NOTE: module standard permission,module: action */
public enum ModuleStandardPermission implements IModuleEnum {
    VIEW(1),
    LIST(1),
    CREATE(2),
    EDIT(3),
    UPDATE(3),
    EXPORT(3),
    IMPORT(3),
    DELETE(4),
    ASSIGN(4),
    PUBLISH(5),
    APPROVE(5),
    MANAGE(5),
    ARCHIVE(6);

    private final int level;

    ModuleStandardPermission(int level) {
        this.level = level;
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public int getLevel() {
        return level;
    }

    public static ModuleStandardPermission fromName(String name) {
        for (ModuleStandardPermission permission : values()) {
            if (permission.name().equalsIgnoreCase(name)) {
                return permission;
            }
        }
        return null;
    }

    public static boolean hasPermission(String currentAction, String requiredAction) {
        ModuleStandardPermission current = fromName(currentAction);
        ModuleStandardPermission required = fromName(requiredAction);
        if (current == null || required == null) {
            return false;
        }
        return current.level >= required.level;
    }
}
