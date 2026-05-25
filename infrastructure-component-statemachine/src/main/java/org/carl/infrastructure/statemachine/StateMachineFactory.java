package org.carl.infrastructure.statemachine;

import org.carl.infrastructure.statemachine.impl.StateMachineException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** StateMachineFactory */
public class StateMachineFactory {
    static Map<String /* machineId */, StateMachine> stateMachineMap = new ConcurrentHashMap<>();

    public static <S, E, C> void register(StateMachine<S, E, C> stateMachine) {
        String machineId = stateMachine.getMachineId();
        StateMachine<?, ?, ?> existing = stateMachineMap.putIfAbsent(machineId, stateMachine);
        if (existing != null) {
            throw new StateMachineException(
                    "The state machine with id ["
                            + machineId
                            + "] is already built, no need to build again");
        }
    }

    public static <S, E, C> StateMachine<S, E, C> get(String machineId) {
        StateMachine stateMachine = stateMachineMap.get(machineId);
        if (stateMachine == null) {
            throw new StateMachineException(
                    "There is no stateMachine instance for "
                            + machineId
                            + ", please build it first");
        }
        return stateMachine;
    }

    public static boolean contains(String machineId) {
        return stateMachineMap.containsKey(machineId);
    }

    public static void unregister(String machineId) {
        stateMachineMap.remove(machineId);
    }
}
