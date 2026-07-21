dependencies {
    implementation(libs.bundles.share)
    implementation(libs.quarkus.pulsar)
    api(project(":infra-component-quarkus:mq"))
    implementation(project(":infra-component-mq-pulsar"))
}
