dependencies {
    api(project(":infra-component-workflow-temporal"))
    // Core types (WorkflowDefinition, WorkflowEvent, NodeHandler, registries) appear directly in
    // this module's public API, so keep the dependency explicit even though temporal also exposes it.
    api(project(":infra-component-workflow-core"))
    // for the io.temporal.client.WorkflowClient CDI bean produced by the quarkiverse extension
    api(libs.quarkus.temporal)
}
