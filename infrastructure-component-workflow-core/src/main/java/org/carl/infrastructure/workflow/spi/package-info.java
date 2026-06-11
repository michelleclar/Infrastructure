/**
 * Service-provider interfaces and shared runtime contracts for custom workflow nodes.
 *
 * <p>Business extensions normally implement {@link org.carl.infrastructure.workflow.spi.NodeHandler}
 * and register through {@link org.carl.infrastructure.workflow.spi.NodeHandlerRegistry}; routing
 * conditions use {@link org.carl.infrastructure.workflow.spi.ConditionEvaluator}.
 */
package org.carl.infrastructure.workflow.spi;
