import org.jooq.meta.kotlin.*

plugins {
    java
    idea
    id("java-library")
    id("io.quarkus") version "3.19.3"
    id("nu.studer.jooq") version "10.1"
}

group = "org.carl"
version = "1.0-SNAPSHOT"

repositories {
    // NOTE: Save bandwidth
    mavenLocal()
//    maven { url = uri("https://maven.aliyun.com/repository/public") }
    mavenCentral()
    maven {
        credentials {
            username = System.getenv("ALIYUN_MAVEN_USERNAME").toString()
            password = System.getenv("ALIYUN_MAVEN_PASSWORD").toString()
        }
        url = uri("https://packages.aliyun.com/659e01070cab697efe1345a8/maven/repo-wdhey")
    }
}
val mainSrc = "src/main/java"
val generatedDir = "src/main/generated"
sourceSets {
    main {
        java {
            srcDirs(generatedDir, mainSrc)
        }
    }
}

idea {
    val mainSrcFile = file("src/main/java")
    val generatedDirFile = file("src/main/generated")
    module {
        generatedSourceDirs.add(generatedDirFile)
        sourceDirs.add(generatedDirFile)
        sourceDirs.add(mainSrcFile)
    }
}

tasks.withType<Test>().configureEach {
    enabled = false
    useJUnitPlatform()
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
    systemProperty("TESTCONTAINERS_REUSE_ENABLE", "true")
}
dependencies {
    implementation(enforcedPlatform(libs.quarkus.platform.bom))
    implementation(libs.infrastructure.component.authorization)
    implementation(libs.infrastructure.component.broadcast)
    implementation(libs.infrastructure.component.cache)
    implementation(libs.infrastructure.component.persistence)
    implementation(libs.infrastructure.component.search)
    implementation(libs.infrastructure.component.web)
    implementation(libs.infrastructure.component.qdrant.grpc)
    implementation(libs.infrastructure.component.embedding.grpc)
    implementation(libs.infrastructure.component.workflow)
    implementation(libs.infrastructure.component.statemachine)
    implementation(libs.infrastructure.component.metrics)
    implementation(libs.infrastructure.component.pulsar)
    jooqGenerator(libs.infrastructure.component.persistence)
    testImplementation(libs.bundles.test)
}

quarkus {
    quarkusBuildProperties.put("quarkus.grpc.codegen.proto-directory", "./protos")
}

// NOTE: fix build pulsar
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
tasks.quarkusBuild {
    nativeArgs {
        "container-build" to true
        "builder-image" to "quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21"
    }
}

jooq {
    version.set("3.20.3")  // default (can be omitted)
    edition.set(nu.studer.gradle.jooq.JooqEdition.OSS)  // default (can be omitted)

    configurations {
        create("main") {  // name of the jOOQ configuration
            // NOTE: native build log manager is diffed
            generateSchemaSourceOnCompilation.set(true)  // default (can be omitted)

            jooqConfiguration {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://localhost:15432/db"
                    user = "root"
                    password = "root"
                    properties {
                        property {
                            key = "ssl"
                            value = "false"
                        }
                    }
                }
                generator {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                        forcedTypes {
                            forcedType {
                                name = "varchar"
                                includeExpression = ".*"
                                includeTypes = "JSONB?"
                            }
                            forcedType {
                                name = "varchar"
                                includeExpression = ".*"
                                includeTypes = "INET"
                            }
                        }
                    }
                    generate {
                        isDeprecated = false
                        isRecords = true
                        isImmutablePojos = true
                        isFluentSetters = true
                        isDaos = true
                    }
                    target {
                        packageName = "org.carl.generated"
                        directory = "src/main/generated"  // default (can be omitted)
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}
