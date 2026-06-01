package org.carl.infrastructure.logging.adapter;

import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LogLevel;
import org.jboss.logging.Logger;

import java.util.IllegalFormatException;

/** JBoss Logging framework adapter. Adapts the JBoss Logger to the unified ILogger interface. */
public record JBossLoggerAdapter(Logger logger) implements ILogger {

    /**
     * Private constructor, use create() method instead
     *
     * @param logger the JBoss logger instance
     */
    public JBossLoggerAdapter {}

    /**
     * Create a JBoss logger adapter for the given class
     *
     * @param clazz the class for which to create the logger
     * @return the logger adapter
     */
    public static JBossLoggerAdapter create(Class<?> clazz) {
        return new JBossLoggerAdapter(Logger.getLogger(clazz));
    }

    /**
     * Create a JBoss logger adapter for the given name
     *
     * @param name the logger name
     * @return the logger adapter
     */
    public static JBossLoggerAdapter create(String name) {
        return new JBossLoggerAdapter(Logger.getLogger(name));
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
        return logger.isEnabled(Logger.Level.WARN);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isEnabled(Logger.Level.ERROR);
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
        logger.trace(formatMessage(format, args));
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
        logger.debug(formatMessage(format, args));
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
        logger.info(formatMessage(format, args));
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
        logger.warn(formatMessage(format, args));
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
        logger.error(formatMessage(format, args));
    }

    @Override
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    private static String formatMessage(String format, Object... args) {
        if (format == null || args == null || args.length == 0) {
            return format;
        }

        if (!format.contains("{}")) {
            try {
                return String.format(format, args);
            } catch (IllegalFormatException ignored) {
                return format;
            }
        }

        StringBuilder message = new StringBuilder(format.length() + args.length * 16);
        int argIndex = 0;
        int searchFrom = 0;
        int placeholder;
        while ((placeholder = format.indexOf("{}", searchFrom)) >= 0 && argIndex < args.length) {
            message.append(format, searchFrom, placeholder);
            message.append(args[argIndex++]);
            searchFrom = placeholder + 2;
        }
        message.append(format, searchFrom, format.length());
        return message.toString();
    }
}
