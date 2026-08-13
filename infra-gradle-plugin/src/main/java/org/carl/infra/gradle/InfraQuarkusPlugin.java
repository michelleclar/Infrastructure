package org.carl.infra.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

/** Applies the shared Java and Quarkus build conventions used by Infra consumers. */
public final class InfraQuarkusPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("java-library");
        project.getPluginManager().apply("idea");
        project.getPluginManager().apply("io.quarkus");

        project.getExtensions().configure(JavaPluginExtension.class, java ->
                java.getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(21)));

        project.getConfigurations().configureEach(configuration ->
                configuration.getResolutionStrategy().cacheChangingModulesFor(0, "seconds"));

        project.getTasks().withType(JavaCompile.class).configureEach(task -> {
            task.getOptions().setEncoding("UTF-8");
            task.getOptions().getCompilerArgs().add("-parameters");
        });

        project.getTasks().withType(Test.class).configureEach(task -> {
            task.useJUnitPlatform();
            task.systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        });
    }
}
