package org.carl.infrastructure.component.web.model;

import org.carl.infrastructure.component.web.runtime.IRuntimeUser;
import org.carl.infrastructure.util.LinkedTable;

/**
 * api path like
 *
 * <p>e.g:
 *
 * <p>GET: /api/v1/{module}.{submodule}/{action}/xxx
 *
 * <p>POST: /api/v1/{module}.{submodule}/{action}
 */
public class ApiRequest {

    public static final ApiRequest BLANK = new ApiRequest("");

    private final String path;
    private String mainModule;
    private String version;
    private LinkedTable<String> models;
    private String action;
    private boolean isSkip = false;
    private IRuntimeUser user = IRuntimeUser.WHITE;
    private String dataId;
    private String moduleID;
    private String moduleName;
    private String actionName;
    private String username;
    private String authCondition;

    private RuntimeException error;

    public ApiRequest(String path) {
        this.path = path;

        // NOTE: /api/v1/{module}.{submodule}/{action}
        // NOTE: /api/v1/system.config/refresh
        String[] urlBlock = path.split("/");
        // ["", "api", "v1", "system.config", "refresh"]

        if (urlBlock.length < 5) {
            isSkip = true;
            return;
        }
        action = urlBlock[4];
        version = urlBlock[2];

        if (urlBlock[3].contains(".")) {
            String[] parts = urlBlock[3].split("\\.");
            models = new LinkedTable<>(parts.length);
            for (String part : parts) {
                models.insert(part);
            }
        } else {
            mainModule = urlBlock[3];
        }
        if (urlBlock.length > 5) {
            dataId = urlBlock[5];
        }
    }

    public ApiRequest setUser(IRuntimeUser user) {
        this.user = user;
        return this;
    }

    public void setSkip() {
        isSkip = true;
    }

    public void setError(RuntimeException error) {
        this.error = error;
    }

    public String getMainModule() {
        return mainModule;
    }

    public String getAction() {
        return action;
    }

    public String getDataId() {
        return this.dataId;
    }

    public IRuntimeUser getUser() {
        return user;
    }

    public boolean isSkip() {
        return isSkip;
    }

    public RuntimeException getError() {
        return error;
    }

    public String getModuleID() {
        return moduleID;
    }

    public ApiRequest setModuleID(String moduleID) {
        this.moduleID = moduleID;
        return this;
    }

    public String getModuleName() {
        return moduleName;
    }

    public ApiRequest setModuleName(String moduleName) {
        this.moduleName = moduleName;
        return this;
    }

    public String getActionName() {
        return actionName;
    }

    public ApiRequest setActionName(String actionName) {
        this.actionName = actionName;
        return this;
    }

    public String getUsername() {
        if (username != null) {
            return username;
        }

        return user.getUsername();
    }

    public ApiRequest setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getAuthCondition() {
        return authCondition;
    }

    public void setAuthCondition(String authCondition) {
        this.authCondition = authCondition;
    }

    @Override
    public String toString() {
        return "path:"
                + path
                + ", module:"
                + mainModule
                + ", models:"
                + models.toString()
                + ", action:"
                + action
                + ", dataId:"
                + dataId
                + ", userId"
                + getUser().getId()
                + ", isSkip:"
                + isSkip;
    }
}
