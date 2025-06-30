plugins {
    java
    idea
    id("com.diffplug.spotless") version "7.0.0.BETA4"
    id("org.kordamp.gradle.jandex") version "2.1.0"
    id("java-library")
}
subprojects {
    apply {
        plugin("com.diffplug.spotless")
        plugin("java")
        plugin("idea")
        plugin("java-library")
        plugin("org.kordamp.gradle.jandex")
    }
    group = "org.carl"
    version = "1.0-BATE"
    repositories {
        // NOTE: Save bandwidth

//        maven { url = uri("https://maven.aliyun.com/repository/public") }
        mavenLocal()
        maven { url = uri("https://mirrors.cloud.tencent.com/repository/maven") }
        mavenCentral()
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
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
        systemProperty("TESTCONTAINERS_REUSE_ENABLE", "true")
    }
    // NOTE: https://github.com/diffplug/spotless/tree/main/plugin-gradle
    spotless {
        ratchetFrom = "origin/main"

        java {
            target("src/main/**/*.java","src/test/**/*.java")
//            importOrder()
            cleanthat()
            googleJavaFormat().aosp()
//            googleJavaFormat().aosp().reflowLongStrings().formatJavadoc(false).reorderImports(true)
//                .groupArtifact("com.google.googlejavaformat:google-java-format")
            formatAnnotations()
            trimTrailingWhitespace()
            endWithNewline()
        }
//        protobuf {
//            target("**/*.proto")
//            targetExclude("**/build/**", "**/build-*/**")
//            buf()
//        }
        flexmark {
            target("**/*.md")
            targetExclude("**/build/**", "**/build-*/**")
            flexmark()
        }
        sql {
            target("src/main/resources/**/*.sql")
            dbeaver()
            prettier()
        }
        yaml {
            target("src/main/resources/**/*.yaml")
            jackson()
            prettier()
        }
        shell {
            target("src/main/resources/**/*.sh")
            shfmt()
        }
    }
//    afterEvaluate {
//        tasks.findByName("spotlessApply")?.let { spotless ->
//            tasks.withType<JavaCompile>().configureEach {
//                finalizedBy(spotless)
//            }
//            tasks.withType<GroovyCompile>().configureEach {
//                finalizedBy(spotless)
//            }
//        }
//    }
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