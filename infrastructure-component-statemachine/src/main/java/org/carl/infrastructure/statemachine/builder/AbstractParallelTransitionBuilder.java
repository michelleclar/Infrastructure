package org.carl.infrastructure.statemachine.builder;

import org.carl.infrastructure.statemachine.State;
import org.carl.infrastructure.statemachine.impl.StateHelper;
import org.carl.infrastructure.statemachine.impl.TransitionType;

import java.util.List;
import java.util.Map;

abstract class AbstractParallelTransitionBuilder<S, E, C>
        implements ParallelFrom<S, E, C>, On<S, E, C>, To<S, E, C> {

    final Map<S, State<S, E, C>> stateMap;

    protected List<State<S, E, C>> targets;

    final TransitionType transitionType;

    public AbstractParallelTransitionBuilder(
            Map<S, State<S, E, C>> stateMap, TransitionType transitionType) {
        this.stateMap = stateMap;
        this.transitionType = transitionType;
    }

    @Override
    public To<S, E, C> toAmong(S... stateIds) {
        targets = StateHelper.getStates(stateMap, stateIds);
        return this;
    }
}
