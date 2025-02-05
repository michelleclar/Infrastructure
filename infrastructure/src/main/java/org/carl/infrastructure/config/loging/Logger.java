package org.carl.infrastructure.config.loging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.message.MessageFormatMessageFactory;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.apache.logging.log4j.spi.AbstractLogger;

@Deprecated
public class Logger extends org.jboss.logging.Logger {

    /**
     * Construct a new instance.
     *
     * @param name the logger category name
     */
    protected Logger(String name) {
        super(name);
        org.apache.logging.log4j.Logger logger = LogManager.getLogger(name);
        if (!(logger instanceof AbstractLogger)) {
            throw new LoggingException(
                    "The logger for ["
                            + name
                            + "] does not extend AbstractLogger. Actual logger: "
                            + logger.getClass().getName());
        }
        this.logger = (AbstractLogger) logger;
        this.messageFactory = new MessageFormatMessageFactory();
    }

    private final AbstractLogger logger;
    private final MessageFormatMessageFactory messageFactory;

    @Override
    protected void doLog(
            final Level level,
            final String loggerClassName,
            final Object message,
            final Object[] parameters,
            final Throwable thrown) {
        final org.apache.logging.log4j.Level translatedLevel = Logger.translate(level);
        if (this.logger.isEnabled(translatedLevel)) {
            try {
                this.logger.logMessage(
                        loggerClassName,
                        translatedLevel,
                        null,
                        (parameters == null || parameters.length == 0)
                                ? this.messageFactory.newMessage(String.valueOf(message))
                                : this.messageFactory.newMessage(
                                        String.valueOf(message), parameters),
                        thrown);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    protected void doLogf(
            final Level level,
            final String loggerClassName,
            final String format,
            final Object[] parameters,
            final Throwable thrown) {
        final org.apache.logging.log4j.Level translatedLevel = Logger.translate(level);
        if (this.logger.isEnabled(translatedLevel)) {
            try {
                this.logger.logMessage(
                        loggerClassName,
                        translatedLevel,
                        null,
                        new StringFormattedMessage(format, parameters),
                        thrown);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public boolean isEnabled(final Level level) {
        return this.logger.isEnabled(Logger.translate(level));
    }

    private static org.apache.logging.log4j.Level translate(final Level level) {
        if (level == Level.TRACE) {
            return org.apache.logging.log4j.Level.TRACE;
        } else if (level == Level.DEBUG) {
            return org.apache.logging.log4j.Level.DEBUG;
        }
        return infoOrHigher(level);
    }

    private static org.apache.logging.log4j.Level infoOrHigher(final Level level) {
        if (level == Level.INFO) {
            return org.apache.logging.log4j.Level.INFO;
        } else if (level == Level.WARN) {
            return org.apache.logging.log4j.Level.WARN;
        } else if (level == Level.ERROR) {
            return org.apache.logging.log4j.Level.ERROR;
        } else if (level == Level.FATAL) {
            return org.apache.logging.log4j.Level.FATAL;
        }
        return org.apache.logging.log4j.Level.ALL;
    }
}
