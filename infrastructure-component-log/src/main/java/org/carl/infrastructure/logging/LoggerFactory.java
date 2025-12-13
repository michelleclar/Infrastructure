package org.carl.infrastructure.logging;

import org.carl.infrastructure.logging.adapter.JBossLoggerAdapter;
import org.carl.infrastructure.logging.adapter.Slf4jLoggerAdapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logging factory that creates logger instances based on configured framework. Supports automatic
 * detection and switching between SLF4J and JBoss Logging.
 */
public class LoggerFactory {

    private static final Map<String, ILogger> loggerCache = new ConcurrentHashMap<>();
    private static volatile LoggingFramework currentFramework = LoggingFramework.AUTO;
    private static volatile LoggingFramework detectedFramework = null;

    /** System property key for configuring logging framework */
    public static final String FRAMEWORK_PROPERTY = "infrastructure.logging.framework";

    /** Environment variable key for configuring logging framework */
    public static final String FRAMEWORK_ENV = "INFRASTRUCTURE_LOGGING_FRAMEWORK";

    /** Private constructor to prevent instantiation */
    private LoggerFactory() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Get logger for the given class
     *
     * @param clazz the class for which to create the logger
     * @return the logger instance
     */
    public static ILogger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * Get logger for the given name
     *
     * @param name the logger name
     * @return the logger instance
     */
    public static ILogger getLogger(String name) {
        return loggerCache.computeIfAbsent(name, LoggerFactory::createLogger);
    }

    /**
     * Set the logging framework to use
     *
     * @param framework the logging framework
     */
    public static void setLoggingFramework(LoggingFramework framework) {
        if (framework == null) {
            throw new IllegalArgumentException("Logging framework cannot be null");
        }
        currentFramework = framework;
        // Clear cache to force recreation of loggers with new framework
        loggerCache.clear();
        detectedFramework = null;
    }

    /**
     * Get the current logging framework
     *
     * @return the current logging framework
     */
    public static LoggingFramework getCurrentFramework() {
        return currentFramework;
    }

    /**
     * Get the detected logging framework
     *
     * @return the detected logging framework
     */
    public static LoggingFramework getDetectedFramework() {
        if (detectedFramework == null) {
            detectedFramework = detectFramework();
        }
        return detectedFramework;
    }

    /** Reset factory to initial state (mainly for testing) */
    public static void reset() {
        loggerCache.clear();
        currentFramework = LoggingFramework.AUTO;
        detectedFramework = null;
    }

    /**
     * Create a logger instance for the given name
     *
     * @param name the logger name
     * @return the logger instance
     */
    private static ILogger createLogger(String name) {
        LoggingFramework framework = resolveFramework();

        return switch (framework) {
            case SLF4J -> createSlf4jLogger(name);
            case JBOSS_LOGGING -> createJBossLogger(name);
            case AUTO ->
                    throw new IllegalStateException("AUTO framework should have been resolved");
        };
    }

    /**
     * Resolve the actual framework to use
     *
     * @return the resolved framework
     */
    private static LoggingFramework resolveFramework() {
        // First, check configuration from system property
        String systemProperty = System.getProperty(FRAMEWORK_PROPERTY);
        if (systemProperty != null && !systemProperty.isEmpty()) {
            try {
                LoggingFramework configured =
                        LoggingFramework.valueOf(systemProperty.toUpperCase());
                if (configured != LoggingFramework.AUTO) {
                    return configured;
                }
            } catch (IllegalArgumentException e) {
                System.err.println(
                        "Invalid logging framework in system property: " + systemProperty);
            }
        }

        // Then, check environment variable
        String envVariable = System.getenv(FRAMEWORK_ENV);
        if (envVariable != null && !envVariable.isEmpty()) {
            try {
                LoggingFramework configured = LoggingFramework.valueOf(envVariable.toUpperCase());
                if (configured != LoggingFramework.AUTO) {
                    return configured;
                }
            } catch (IllegalArgumentException e) {
                System.err.println(
                        "Invalid logging framework in environment variable: " + envVariable);
            }
        }

        // Use explicitly set framework
        if (currentFramework != LoggingFramework.AUTO) {
            return currentFramework;
        }

        // Auto-detect available framework
        return getDetectedFramework();
    }

    /**
     * Detect available logging framework
     *
     * @return the detected framework
     */
    private static LoggingFramework detectFramework() {
        // Try JBoss Logging first (preferred for Quarkus)
        if (isJBossLoggingAvailable()) {
            return LoggingFramework.JBOSS_LOGGING;
        }

        // Fallback to SLF4J
        if (isSlf4jAvailable()) {
            return LoggingFramework.SLF4J;
        }

        throw new IllegalStateException(
                "No logging framework detected. Please ensure either SLF4J or JBoss Logging is on the classpath.");
    }

    /**
     * Check if SLF4J is available
     *
     * @return true if SLF4J is available
     */
    private static boolean isSlf4jAvailable() {
        try {
            Class.forName("org.slf4j.LoggerFactory");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Check if JBoss Logging is available
     *
     * @return true if JBoss Logging is available
     */
    private static boolean isJBossLoggingAvailable() {
        try {
            Class.forName("org.jboss.logging.Logger");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Create a SLF4J logger
     *
     * @param name the logger name
     * @return the logger instance
     */
    private static ILogger createSlf4jLogger(String name) {
        try {
            return Slf4jLoggerAdapter.create(name);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create SLF4J logger for: " + name, e);
        }
    }

    /**
     * Create a JBoss logger
     *
     * @param name the logger name
     * @return the logger instance
     */
    private static ILogger createJBossLogger(String name) {
        try {
            return JBossLoggerAdapter.create(name);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create JBoss logger for: " + name, e);
        }
    }
}
