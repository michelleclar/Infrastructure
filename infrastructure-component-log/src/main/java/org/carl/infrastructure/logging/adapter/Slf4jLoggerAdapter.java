package org.carl.infrastructure.logging.adapter;

import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SLF4J logging framework adapter. Adapts the SLF4J Logger to the unified ILogger interface. */
public record Slf4jLoggerAdapter(Logger logger) implements ILogger {

    /**
     * Private constructor, use create() method instead
     *
     * @param logger the SLF4J logger instance
     */
    public Slf4jLoggerAdapter {}

    /**
     * Create a SLF4J logger adapter for the given class
     *
     * @param clazz the class for which to create the logger
     * @return the logger adapter
     */
    public static Slf4jLoggerAdapter create(Class<?> clazz) {
        return new Slf4jLoggerAdapter(LoggerFactory.getLogger(clazz));
    }

    /**
     * Create a SLF4J logger adapter for the given name
     *
     * @param name the logger name
     * @return the logger adapter
     */
    public static Slf4jLoggerAdapter create(String name) {
        return new Slf4jLoggerAdapter(LoggerFactory.getLogger(name));
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public boolean isEnabled(LogLevel level) {
        return switch (level) {
            case TRACE -> isTraceEnabled();
            case DEBUG -> isDebugEnabled();
            case INFO -> isInfoEnabled();
            case WARN -> isWarnEnabled();
            case ERROR -> isErrorEnabled();
        };
    }

    @Override
    public void trace(String message) {
        logger.trace(message);
    }

    @Override
    public void trace(String format, Object... args) {
        logger.trace(format, args);
    }

    @Override
    public void trace(String message, Throwable throwable) {
        logger.trace(message, throwable);
    }

    @Override
    public void debug(String message) {
        logger.debug(message);
    }

    @Override
    public void debug(String format, Object... args) {
        logger.debug(format, args);
    }

    @Override
    public void debug(String message, Throwable throwable) {
        logger.debug(message, throwable);
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void info(String format, Object... args) {
        logger.info(format, args);
    }

    @Override
    public void info(String message, Throwable throwable) {
        logger.info(message, throwable);
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
    }

    @Override
    public void warn(String format, Object... args) {
        logger.warn(format, args);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        logger.warn(message, throwable);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void error(String format, Object... args) {
        logger.error(format, args);
    }

    @Override
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }
}
