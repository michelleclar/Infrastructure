plugins {
    java
    idea
    id("com.diffplug.spotless") version "7.0.0.BETA4"
    id("java-library")
}
subprojects {
    apply {
        plugin("com.diffplug.spotless")
        plugin("java")
        plugin("idea")
        plugin("java-library")
    }
    group = "org.carl"
    version = "1.0-BATE"
    repositories {
        // NOTE: Save bandwidth
        mavenLocal()
        maven { url = uri("https://maven.aliyun.com/repository/central") }
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
        enabled = false
        useJUnitPlatform()
        systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
        systemProperty("TESTCONTAINERS_REUSE_ENABLE", "true")
    }
    // NOTE: https://github.com/diffplug/spotless/tree/main/plugin-gradle
    spotless {
        ratchetFrom = "origin/main"

        java {
            importOrder()
            cleanthat()
            googleJavaFormat().aosp().reflowLongStrings().formatJavadoc(false).reorderImports(false)
                .groupArtifact("com.google.googlejavaformat:google-java-format")
            formatAnnotations()
            trimTrailingWhitespace()
            endWithNewline()
        }
        protobuf {
            target("**/*.proto")
            buf()
            licenseHeader("/* (C) \$YEAR */")
        }
        flexmark {
            target("**/*.md")
            flexmark()
        }
        sql {
            target("src/main/resources/**/*.sql")
            targetExclude("**/build/**", "**/build-*/**")
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
        groovyGradle {
            target("**/*.gradle")
            greclipse()
            indentWithSpaces(4)
            trimTrailingWhitespace()
            endWithNewline()
        }
//        format 'xml', {
//            target fileTree('.') {
//                include '**/*.xml'
//                exclude '**/build/**', '**/build-*/**'
//            }
//            eclipseWtp('xml')
//            trimTrailingWhitespace()
//            indentWithSpaces(2)
//            endWithNewline()
//        }
//        format 'misc', {
//            target fileTree('.') {
//                include '**/*.md', '**/.gitignore'
//                exclude '**/build/**', '**/build-*/**'
//            }
//            trimTrailingWhitespace()
//            indentWithSpaces(2)
//            endWithNewline()
//        }
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
    // NOTE: pulsar
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
//    val libs = rootProject.libs
//    dependencies {
//        implementation(enforcedPlatform(libs.quarkus.platform.bom))
//        testImplementation(libs.bundles.test)
//    }
//    if (!project.toString().contains("infrastructure")) {
//        dependencies {
////            implementation project(':infrastructure-components:infrastructure-component-boot')
//            implementation(project(":infrastructure-components:infrastructure-component-authorization"))
//            implementation(project(":infrastructure-components:infrastructure-component-broadcast"))
//            implementation(project(":infrastructure-components:infrastructure-component-cache"))
//            implementation(project(":infrastructure-components:infrastructure-component-persistence"))
//            implementation(project(":infrastructure-components:infrastructure-component-search"))
////            implementation project(':infrastructure-components:infrastructure-component-tool')
//            implementation(project(":infrastructure-components:infrastructure-component-web"))
//        }
//        val mainSrc = "src/main/java"
//        val generatedDir = "src/main/generated"
//        sourceSets {
//            named("main") {
//                java {
//                    srcDir(generatedDir)
//                    srcDir(mainSrc)
//                }
//            }
//        }
//
//        idea {
//            val mainSrcFile = file("src/main/java")
//            val generatedDirFile = file("src/main/generated")
//            module {
//                generatedSourceDirs.add(generatedDirFile)
//                sourceDirs.add(generatedDirFile)
//                sourceDirs.add(mainSrcFile)
//            }
//        }
////        var api = System.getenv("API_PREFIX") == null ? "/v1/api" : System.getenv("API_PREFIX")
//        quarkus {
//            quarkusBuildProperties.put("quarkus.grpc.codegen.proto-directory", "./protos")
////            quarkusBuildProperties.put("quarkus.http.root-path", api)
//        }
//        tasks.quarkusBuild {
//            nativeArgs {
//                "container-build" to true
//                "builder-image" to "quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21"
//
//            }
//        }
//    }
}