package org.carl.infrastructure.statemachine;

import java.util.List;

/**
 * StateMachine
 *
 * @param <S> the type of state
 * @param <E> the type of event
 * @param <C> the user defined context
 */
public interface StateMachine<S, E, C> extends Visitable {

    /**
     * Verify if an event {@code E} can be fired from current state {@code S}
     *
     * @param sourceStateId
     * @param event
     * @return
     */
    boolean verify(S sourceStateId, E event);

    /**
     * Send an event {@code E} to the state machine.
     *
     * @param sourceState the source state
     * @param event the event to send
     * @param ctx the user defined context
     * @return the target state
     */
    S fireEvent(S sourceState, E event, C ctx);

    List<S> fireParallelEvent(S sourceState, E event, C ctx);

    /**
     * MachineId is the identifier for a State Machine
     *
     * @return
     */
    String getMachineId();

    /** Use visitor pattern to display the structure of the state machine */
    void showStateMachine();

    String generatePlantUML();
}
