package org.carl.infra.gradle;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InfraQuarkusPluginFunctionalTest {

    @TempDir
    Path projectDir;

    @Test
    void appliesJavaIdeaQuarkusAndJava21Conventions() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'plugin-smoke-test'\n");
        Files.writeString(projectDir.resolve("build.gradle"), """
                plugins {
                    id 'org.carl.infra.quarkus'
                }

                tasks.register('verifyInfraConvention') {
                    doLast {
                        assert pluginManager.hasPlugin('java-library')
                        assert pluginManager.hasPlugin('idea')
                        assert pluginManager.hasPlugin('io.quarkus')
                        assert java.toolchain.languageVersion.get().asInt() == 21
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("verifyInfraConvention", "--stacktrace")
                .build();

        assertEquals(SUCCESS, result.task(":verifyInfraConvention").getOutcome());
    }
}
