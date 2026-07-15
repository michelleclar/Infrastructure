/**
 * Service-provider interfaces and shared runtime contracts for custom workflow nodes.
 *
 * <p>Business extensions normally implement {@link org.carl.infra.workflow.spi.NodeHandler}
 * and register through {@link org.carl.infra.workflow.spi.NodeHandlerRegistry}; routing
 * conditions use {@link org.carl.infra.workflow.spi.ConditionEvaluator}.
 */
package org.carl.infra.workflow.spi;
