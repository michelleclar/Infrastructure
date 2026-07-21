dependencies {
    implementation(libs.bundles.share)
    implementation("io.quarkus:quarkus-kafka-client")
    api(project(":infra-component-quarkus:mq"))
    implementation(project(":infra-component-mq-kafka"))
}
