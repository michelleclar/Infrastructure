package org.carl.infrastructure.statemachine;

public class ActionWrapperChain {

    @SafeVarargs
    public static <S, E, C> Action<S, E, C> chain(
            Action<S, E, C> base, ActionWrapper<S, E, C>... wrappers) {
        Action<S, E, C> current = base;
        for (int i = wrappers.length - 1; i >= 0; i--) {
            current = wrappers[i].wrap(current);
        }
        return current;
    }
}
