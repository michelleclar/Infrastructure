package org.carl.infra.statemachine;

/**
 * Visitable
 *
 */
public interface Visitable {
    String accept(final Visitor visitor);
}
