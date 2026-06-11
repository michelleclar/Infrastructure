/**
 * Fluent authoring API that compiles Java builder calls into {@code definition} package records.
 *
 * <p>The DSL layer is deliberately runtime-free: it only accumulates node configs, edges, and
 * task-group join specs, then emits a serializable {@link
 * org.carl.infrastructure.workflow.definition.WorkflowDefinition}.
 */
package org.carl.infrastructure.workflow.dsl;
