/**
 * Lifecycle hook contracts around workflow execution.
 *
 * <p>Deterministic interceptors run inline and must obey replay-safety rules. Async interceptors
 * are intended to run through the runtime adapter's side-effect boundary.
 */
package org.carl.infrastructure.workflow.interceptor;
