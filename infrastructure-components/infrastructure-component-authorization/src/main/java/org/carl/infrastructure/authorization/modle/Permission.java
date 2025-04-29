package org.carl.infrastructure.authorization.modle;

import java.util.Set;
import java.util.stream.Collectors;
import org.carl.infrastructure.authorization.IModuleEnum;

public class Permission {
    final String module;
    final Set<ModuleAction> actions;
    Integer maxLevel = -1;
    Set<String> enabledActions;
    Set<String> disabledActions;

    public Permission(String module, Set<ModuleAction> actions) {
        this.module = module;
        this.actions = actions;
    }

    public Boolean hasPermission(String requirePermission) {
        requirePermission = requirePermission.toUpperCase();
        if (getDisabledActions().contains(requirePermission)) {
            return false;
        }
        return enabledActions.contains(requirePermission);
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
}
