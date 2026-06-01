# infrastructure-component-quarkus/workflow

Quarkus adapter that wires the generic Temporal workflow engine into a CDI application.
Registers all `ProcessDefinition` beans at startup, starts a single Temporal worker, and
exposes `WorkflowFacade` for callers to start, signal, vote, and query process instances.

## Enabling DEBUG logging

Add the following to your `application.properties` (or `application.yaml`) to enable full
engine-level debug output at runtime:

```properties
quarkus.log.category."org.carl.infrastructure.workflow".level=DEBUG
```

For TRACE-level facade query logging, use `TRACE` instead of `DEBUG`.
