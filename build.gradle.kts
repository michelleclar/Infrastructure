plugins {
    idea
    id("org.kordamp.gradle.jandex") version "2.1.0"
}

tasks.named("jandex") {
    enabled = false
}
subprojects {
    apply {
        plugin("java")
        plugin("idea")
        plugin("java-library")
        plugin("org.kordamp.gradle.jandex")
    }
    group = "org.carl"
    version = "1.0-BATE"
    repositories {
        mavenCentral()
        mavenLocal()
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Javadoc> {
        enabled = false
    }

    tasks.withType<GenerateModuleMetadata>().configureEach {
        suppressedValidationErrors.add("enforced-platform")
    }

    tasks.compileTestJava {
        options.encoding = "UTF-8"
    }

    tasks.withType<Javadoc>().configureEach {
        options.encoding = "UTF-8"
    }

    // NOTE: build pulsar
    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.apache.bookkeeper:circe-checksum"))
                .using(module("org.apache.bookkeeper:circe-checksum:4.17.0"))
                .withoutClassifier()
            substitute(module("org.apache.bookkeeper:cpu-affinity"))
                .using(module("org.apache.bookkeeper:cpu-affinity:4.17.0"))
                .withoutClassifier()
        }
    }
}
