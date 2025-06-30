package org.carl.infrastructure.authorization.modle;

import org.carl.infrastructure.authorization.IModuleEnum;

public class ModuleAction {

    String action;
    Integer level;
    Boolean enable;

    ModuleAction(String action, Integer level, Boolean enable) {
        this.action = action;
        this.level = level;
        this.enable = enable;
    }

    public static class ModuleActionBuilder {
        private String action;
        private Integer level;
        private Boolean enable;

        public ModuleActionBuilder() {}

        public static ModuleActionBuilder create() {
            return new ModuleActionBuilder();
        }

        public ModuleActionBuilder action(String action) {
            this.action = action;
            return this;
        }

        public ModuleActionBuilder level(Integer level) {
            this.level = level;
            return this;
        }

        public ModuleActionBuilder enable(Boolean enable) {
            this.enable = enable;
            return this;
        }

        public <T extends Enum<T> & IModuleEnum> ModuleActionBuilder addStandardAction(T module) {
            this.action = module.getName();
            this.level = module.getLevel();
            return this;
        }

        public ModuleAction build() {
            return new ModuleAction(action, level, enable);
        }
    }
}
