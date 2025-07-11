package org.carl.infrastructure.statemachine;

/**
 * Visitor
 *
 */
public interface Visitor {

    char LF = '\n';

    /**
     * @param visitable the element to be visited.
     * @return
     */
    String visitOnEntry(StateMachine<?, ?, ?> visitable);

    /**
     * @param visitable the element to be visited.
     * @return
     */
    String visitOnExit(StateMachine<?, ?, ?> visitable);

    /**
     * @param visitable the element to be visited.
     * @return
     */
    String visitOnEntry(State<?, ?, ?> visitable);

    /**
     * @param visitable the element to be visited.
     * @return
     */
    String visitOnExit(State<?, ?, ?> visitable);
}
