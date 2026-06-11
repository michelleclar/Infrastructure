/**
 * Read-only graph indexes and structural validation for workflow definitions.
 *
 * <p>The graph layer knows topology only. It can answer reachability, start/end nodes, outgoing
 * edges, and cycles, but it does not evaluate runtime conditions because those need a {@code
 * NodeExecutionContext}.
 */
package org.carl.infrastructure.workflow.graph;
