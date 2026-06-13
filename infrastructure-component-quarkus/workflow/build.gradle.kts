dependencies {
    api(project(":infrastructure-component-workflow-temporal"))
    // core types (WorkflowDefinition, WorkflowEvent, NodeHandler, registries) appear in this
    // module's public API (WorkflowFacade / WorkflowBootstrap); temporal depends on core via
    // `implementation`, so it is not transitive — declare it directly as `api`.
    api(project(":infrastructure-component-workflow-core"))
    // for the io.temporal.client.WorkflowClient CDI bean produced by the quarkiverse extension
    api(libs.quarkus.temporal)
}
