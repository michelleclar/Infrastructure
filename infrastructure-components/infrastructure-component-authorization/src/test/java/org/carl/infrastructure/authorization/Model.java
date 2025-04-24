package org.carl.infrastructure.authorization;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.carl.infrastructure.authorization.ModulePermission.ModulePermissionBuilder;

public class Model implements IModel {
    String name;
    String description;
    String url;
    String code;
    Boolean isVisible;
    Boolean isRoot;
    Set<IModel> children = new HashSet<>();
    Set<ModulePermission> permissions = new HashSet<>();

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public List<ModulePermission> getPermissions() {
        return List.of();
    }

    @Override
    public List<IModel> getSubModules() {
        return List.of();
    }

    public static class ModuleBuild {
        private final Model model;

        ModuleBuild() {
            this.model = new Model();
        }

        public static ModuleBuild create() {
            return new ModuleBuild();
        }

        public ModuleBuild setName(String name) {
            model.name = name;
            return this;
        }

        public ModuleBuild setDescription(String description) {
            model.description = description;
            return this;
        }

        public ModuleBuild setUrl(String url) {
            model.url = url;
            return this;
        }

        public ModuleBuild setCode(String code) {
            model.code = code;
            return this;
        }

        public ModuleBuild setIsVisible(Boolean isVisible) {
            model.isVisible = isVisible;
            return this;
        }

        public ModuleBuild setIsRoot(Boolean isRoot) {
            model.isRoot = isRoot;
            return this;
        }

        public ModuleBuild addPermissions(Function<ModulePermissionBuilder, ModulePermission> f) {
            ModulePermission permission = f.apply(new ModulePermissionBuilder());
            model.permissions.add(permission);
            return this;
        }

        public ModuleBuild addSubModules(Function<ModuleBuild, Model> f) {
            Model m = f.apply(new ModuleBuild());
            model.children.add(m);
            return this;
        }

        public ModuleBuild setIsVisible() {
            model.isVisible = true;
            return this;
        }
        public Model build(){
            return model;
        }
    }
}
