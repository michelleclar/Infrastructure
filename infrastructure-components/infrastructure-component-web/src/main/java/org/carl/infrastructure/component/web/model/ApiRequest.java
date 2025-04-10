package org.carl.infrastructure.component.web.model;

import org.carl.infrastructure.component.web.runtime.IRuntimeUser;

import java.util.Map;

/** api path like /api/v1/{module}.{submodule}/{action} */
public class ApiRequest {

    public static final ApiRequest BLANK = new ApiRequest("");

    private final String path;
    private String module;
    private String action;
    private Map<String, Object> parameters;
    private String dataID;
    private boolean isSkip = false;
    private IRuntimeUser user = IRuntimeUser.WHITE;
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

        module = urlBlock[3];
        action = urlBlock[4];

        if (urlBlock.length > 5) {
            dataID = urlBlock[5];
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

    public String getModule() {
        return module;
    }

    public String getAction() {
        return action;
    }

    public String getDataID() {
        return dataID;
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
        if (username != null) { // sso 登录的时候，会手动写入username
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
                + module
                + ", action:"
                + action
                + ", dataID:"
                + dataID
                + ", userID"
                + getUser().getId()
                + ", isSkip:"
                + isSkip;
    }
}
