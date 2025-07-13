dependencies {
    api(libs.quarkus.temporal)
    api(project(":infrastructure-component-statemachine"))
    testImplementation(libs.quarkus.temporal.test)
}
