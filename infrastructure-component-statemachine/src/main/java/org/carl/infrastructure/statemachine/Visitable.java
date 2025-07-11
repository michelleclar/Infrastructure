package org.carl.infrastructure.statemachine;

/**
 * Visitable
 *
 */
public interface Visitable {
    String accept(final Visitor visitor);
}
