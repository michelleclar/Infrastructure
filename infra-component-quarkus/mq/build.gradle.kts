dependencies {
    implementation(libs.bundles.share)
    implementation("io.quarkus:quarkus-kafka-client")
    api(project(":infra-component-mq-api"))
    api(project(":infra-component-mq-kafka"))
}
