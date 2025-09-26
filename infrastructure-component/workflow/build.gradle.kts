import org.jooq.meta.jaxb.MatcherTransformType
import org.jooq.meta.kotlin.*

plugins {
    id("nu.studer.jooq") version "10.1"
}
dependencies {
    api(libs.quarkus.temporal)
    api(project(":infrastructure-component-statemachine"))
    implementation(project(":infrastructure-component:persistence"))
    implementation(project(":infrastructure-component:pulsar"))
    jooqGenerator(project(":infrastructure-component:persistence"))
    testImplementation(libs.quarkus.temporal.test)
}

jooq {
    version.set("3.20.3")  // default (can be omitted)
    edition.set(nu.studer.gradle.jooq.JooqEdition.OSS)  // default (can be omitted)

    configurations {
        create("main") {  // name of the jOOQ configuration
            // NOTE: native build log manager is diffed
            generateSchemaSourceOnCompilation.set(false)  // default (can be omitted)

            jooqConfiguration {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://180.184.66.147:15432/db"
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
                        inputSchema = "workflow"
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
                        isImmutablePojos = false
                        isFluentSetters = true
                        isPojos = true
                    }
                    target {
                        packageName = "org.carl.infrastructure.workflow.gen"
                        directory = "src/main/gen"
                    }
                    strategy {
                        name = "org.jooq.codegen.DefaultGeneratorStrategy"
                        matchers {
                            tables {
                                table {
                                    pojoClass {
                                        transform = MatcherTransformType.PASCAL
                                        expression = "\$0_DO"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
