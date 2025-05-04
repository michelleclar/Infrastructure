package org.carl.infrastructure.authorization.modle;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.carl.infrastructure.authorization.IModuleEnum;
import org.carl.infrastructure.authorization.modle.ModuleAction.ModuleActionBuilder;

/**
 * user all permission
 *
 * <p>1. module permission
 *
 * <p>2. resource permission
 */
public class Permission {
    private final String module;

    private Set<ModuleAction> actions;
    private Integer maxLevel = -1;
    private Set<String> enabledActions;
    private Set<String> disabledActions;

    private Permission(String module) {
        this.module = module;
    }

    public Permission(String module, Set<ModuleAction> actions) {
        this.module = module;
        this.actions = actions;
    }

    public Boolean hasPermission(String requirePermission) {
        requirePermission = requirePermission.toUpperCase();
        if (getDisabledActions().contains(requirePermission)) {
            return false;
        }
        return getEnabledActions().contains(requirePermission);
    }

    public Boolean hasPermission(Integer permissionLevel) {
        return permissionLevel >= getMaxLevel();
    }

    public <T extends Enum<T> & IModuleEnum> Boolean hasPermission(T requiredPermission) {
        return hasPermission(requiredPermission, true);
    }

    public <T extends Enum<T> & IModuleEnum> Boolean hasPermission(
            T requiredPermission, Boolean ignoreLevel) {
        return ignoreLevel
                ? hasPermission(requiredPermission.name())
                : hasPermission(requiredPermission.getLevel());
    }

    public Set<String> getEnabledActions() {
        if (enabledActions == null) {
            enabledActions =
                    actions.stream()
                            .filter(moduleAction -> moduleAction.enable)
                            .map(moduleAction -> moduleAction.action.toUpperCase())
                            .collect(Collectors.toSet());
        }
        return enabledActions;
    }

    public Set<String> getDisabledActions() {
        if (disabledActions == null) {
            disabledActions =
                    actions.stream()
                            .filter(moduleAction -> !moduleAction.enable)
                            .map(moduleAction -> moduleAction.action.toUpperCase())
                            .collect(Collectors.toSet());
        }
        return disabledActions;
    }

    public Integer getMaxLevel() {
        if (maxLevel == null) {
            actions.forEach(action -> maxLevel = Math.max(maxLevel, action.level));
        }
        return maxLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Permission that = (Permission) o;
        return module.equals(that.module);
    }

    @Override
    public int hashCode() {
        return module.hashCode();
    }

    public static class PermissionBuilder {
        Permission permission;

        private PermissionBuilder(String module) {
            permission = new Permission(module);
        }

        public static PermissionBuilder create(String module) {
            return new PermissionBuilder(module);
        }

        public PermissionBuilder addAction(Function<ModuleActionBuilder, ModuleAction> f) {
            ModuleAction apply = f.apply(new ModuleActionBuilder());
            addActions(apply);
            return this;
        }

        public PermissionBuilder addAction(ModuleAction action) {
            addActions(action);
            return this;
        }

        public Permission build() {
            return permission;
        }

        private void addActions(ModuleAction action) {
            if (permission.actions == null) {
                permission.actions = new HashSet<>();
            }
            permission.actions.add(action);
        }
    }
}
