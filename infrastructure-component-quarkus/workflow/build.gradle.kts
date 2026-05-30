dependencies {
    api(project(":infrastructure-component-workflow-temporal"))
    // for the io.temporal.client.WorkflowClient CDI bean produced by the quarkiverse extension
    api(libs.quarkus.temporal)
}
