import org.jooq.meta.kotlin.*

plugins {
    java
    idea
    id("io.quarkus") version "3.19.3"
    id("nu.studer.jooq") version "10.1"
}

group = "org.carl"
version = "1.0-SNAPSHOT"

repositories {
    // NOTE: Save bandwidth
    mavenLocal()
    maven { url = uri("https://maven.aliyun.com/repository/central") }
    maven {
        credentials {
            username = findProperty("MAVEN_USERNAME").toString()
            password = findProperty("MAVEN_PASSWORD").toString()
        }
        url = uri("https://packages.aliyun.com/659e01070cab697efe1345a8/maven/repo-wdhey")
    }
}
val mainSrc = "src/main/java"
val generatedDir = "src/main/generated"
sourceSets {
    named("main") {
        java {
            srcDir(generatedDir)
            srcDir(mainSrc)
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
    implementation(libs.infrastructure.component.web)
    implementation(libs.infrastructure.component.persistence)
    implementation(libs.infrastructure.component.qdrant.grpc)
    implementation(libs.infrastructure.component.embedding.grpc)
    implementation(libs.infrastructure.component.dto)
    implementation("com.hankcs:hanlp:portable-1.8.6")
    implementation("org.ansj:ansj_seg:5.1.6")
    testImplementation(libs.bundles.test)
    jooqGenerator(libs.infrastructure.component.persistence)
}

quarkus {
    quarkusBuildProperties.put("quarkus.grpc.codegen.proto-directory", "./protos")
}

tasks.quarkusBuild {
    nativeArgs {
        "quarkus.native.container-runtime-options" to "--platform=linux/amd64"
        "container-build" to true
//        "builder-image" to "quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21"
    }
}
tasks.processResources {
    from("src/main/resources") {
        include("library/**")
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
                                name = "FLOAT"
                                includeTypes = "FLOAT8\$$(\$\$)?"
                                userType = "java.lang.Float[]"
                            }
                            forcedType {
                                userType = "java.lang.Float[]"
                                includeTypes = "vector*"
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
