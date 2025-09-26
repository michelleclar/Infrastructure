package org.carl.infrastructure.workflow.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "workflow")
public interface WorkflowArgsConfig {
    Enable enable();

    interface Enable {
        @WithDefault("true")
        @WithName("log")
        boolean log();
    }
}
