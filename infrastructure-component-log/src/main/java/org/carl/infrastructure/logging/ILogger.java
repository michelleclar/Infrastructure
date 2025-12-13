package org.carl.infrastructure.logging;

/**
 * Unified logging interface facade. Provides a consistent API for logging operations regardless of
 * the underlying logging framework.
 */
public interface ILogger {

    /**
     * Get logger name
     *
     * @return logger name
     */
    String getName();

    /**
     * Check if TRACE level is enabled
     *
     * @return true if TRACE is enabled
     */
    boolean isTraceEnabled();

    /**
     * Check if DEBUG level is enabled
     *
     * @return true if DEBUG is enabled
     */
    boolean isDebugEnabled();

    /**
     * Check if INFO level is enabled
     *
     * @return true if INFO is enabled
     */
    boolean isInfoEnabled();

    /**
     * Check if WARN level is enabled
     *
     * @return true if WARN is enabled
     */
    boolean isWarnEnabled();

    /**
     * Check if ERROR level is enabled
     *
     * @return true if ERROR is enabled
     */
    boolean isErrorEnabled();

    /**
     * Check if a specific log level is enabled
     *
     * @param level log level
     * @return true if the level is enabled
     */
    boolean isEnabled(LogLevel level);

    // ==================== TRACE Level ====================

    /**
     * Log a message at TRACE level
     *
     * @param message the message to log
     */
    void trace(String message);

    /**
     * Log a formatted message at TRACE level
     *
     * @param format the format string
     * @param args the arguments
     */
    void trace(String format, Object... args);

    /**
     * Log a message with throwable at TRACE level
     *
     * @param message the message to log
     * @param throwable the exception to log
     */
    void trace(String message, Throwable throwable);

    // ==================== DEBUG Level ====================

    /**
     * Log a message at DEBUG level
     *
     * @param message the message to log
     */
    void debug(String message);

    /**
     * Log a formatted message at DEBUG level
     *
     * @param format the format string
     * @param args the arguments
     */
    void debug(String format, Object... args);

    /**
     * Log a message with throwable at DEBUG level
     *
     * @param message the message to log
     * @param throwable the exception to log
     */
    void debug(String message, Throwable throwable);

    // ==================== INFO Level ====================

    /**
     * Log a message at INFO level
     *
     * @param message the message to log
     */
    void info(String message);

    /**
     * Log a formatted message at INFO level
     *
     * @param format the format string
     * @param args the arguments
     */
    void info(String format, Object... args);

    /**
     * Log a message with throwable at INFO level
     *
     * @param message the message to log
     * @param throwable the exception to log
     */
    void info(String message, Throwable throwable);

    // ==================== WARN Level ====================

    /**
     * Log a message at WARN level
     *
     * @param message the message to log
     */
    void warn(String message);

    /**
     * Log a formatted message at WARN level
     *
     * @param format the format string
     * @param args the arguments
     */
    void warn(String format, Object... args);

    /**
     * Log a message with throwable at WARN level
     *
     * @param message the message to log
     * @param throwable the exception to log
     */
    void warn(String message, Throwable throwable);

    // ==================== ERROR Level ====================

    /**
     * Log a message at ERROR level
     *
     * @param message the message to log
     */
    void error(String message);

    /**
     * Log a formatted message at ERROR level
     *
     * @param format the format string
     * @param args the arguments
     */
    void error(String format, Object... args);

    /**
     * Log a message with throwable at ERROR level
     *
     * @param message the message to log
     * @param throwable the exception to log
     */
    void error(String message, Throwable throwable);
}
