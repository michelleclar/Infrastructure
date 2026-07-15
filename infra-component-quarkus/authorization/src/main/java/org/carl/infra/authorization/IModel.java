package org.carl.infra.authorization;

import java.util.List;

public interface IModel {

    String getUrl();

    String getName();

    String getCode();

    String getDescription();

    boolean isVisible();

    boolean isRoot();

    List<ModulePermission> getPermissions();

    List<IModel> getSubModules();
}
