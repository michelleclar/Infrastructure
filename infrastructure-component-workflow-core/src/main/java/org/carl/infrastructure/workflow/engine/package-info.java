/**
 * Platform-independent engine helpers shared by runtime adapters and tests.
 *
 * <p>Classes here make pure decisions over already-built definitions: config normalization/decoding
 * and edge routing. They must not call Temporal, databases, network clients, or other side-effect
 * APIs.
 */
package org.carl.infrastructure.workflow.engine;
