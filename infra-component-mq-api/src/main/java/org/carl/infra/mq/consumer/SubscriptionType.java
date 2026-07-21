package org.carl.infra.mq.consumer;

/**
 * Subscription distribution capability.
 *
 * <p>Concrete capability objects are supplied by the common API or by a provider module. Provider
 * implementations must reject unsupported objects instead of silently ignoring them.
 */
public interface SubscriptionType {}
