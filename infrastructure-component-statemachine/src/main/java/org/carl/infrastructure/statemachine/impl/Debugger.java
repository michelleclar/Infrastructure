package org.carl.infrastructure.statemachine.impl;

/**
 * Debugger, This is used to decouple Logging framework dependency
 *
 */
public class Debugger {

    private static boolean isDebugOn = false;

    public static void debug(String message) {
        if (isDebugOn) {
            System.out.println(message);
        }
    }

    public static void enableDebug() {
        isDebugOn = true;
    }
}
