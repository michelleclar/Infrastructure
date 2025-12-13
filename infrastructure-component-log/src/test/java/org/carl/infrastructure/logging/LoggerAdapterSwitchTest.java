package org.carl.infrastructure.logging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/** Tests for logging adapter switching between SLF4J and JBoss Logging */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoggerAdapterSwitchTest {

    @BeforeEach
    void setUp() {
        LoggerFactory.reset();
    }

    @AfterEach
    void tearDown() {
        LoggerFactory.reset();
    }

    @Test
    @Order(1)
    @DisplayName("Should create SLF4J adapter and log messages")
    void testSlf4jAdapter() {
        LoggerFactory.setLoggingFramework(LoggingFramework.SLF4J);
        ILogger logger = LoggerFactory.getLogger("test.slf4j.adapter");

        assertNotNull(logger, "SLF4J logger should be created");
        assertEquals("test.slf4j.adapter", logger.getName(), "Logger name should match");

        // Test all log levels
        assertDoesNotThrow(
                () -> {
                    logger.trace("SLF4J TRACE message");
                    logger.debug("SLF4J DEBUG message");
                    logger.info("SLF4J INFO message");
                    logger.warn("SLF4J WARN message");
                    logger.error("SLF4J ERROR message");
                },
                "SLF4J adapter should handle all log levels");
    }

    @Test
    @Order(2)
    @DisplayName("Should create JBoss Logging adapter and log messages")
    void testJBossAdapter() {
        LoggerFactory.setLoggingFramework(LoggingFramework.JBOSS_LOGGING);
        ILogger logger = LoggerFactory.getLogger("test.jboss.adapter");

        assertNotNull(logger, "JBoss logger should be created");
        assertEquals("test.jboss.adapter", logger.getName(), "Logger name should match");

        // Test all log levels
        assertDoesNotThrow(
                () -> {
                    logger.trace("JBoss TRACE message");
                    logger.debug("JBoss DEBUG message");
                    logger.info("JBoss INFO message");
                    logger.warn("JBoss WARN message");
                    logger.error("JBoss ERROR message");
                },
                "JBoss adapter should handle all log levels");
    }

    @Test
    @Order(3)
    @DisplayName("Should switch from SLF4J to JBoss Logging")
    void testSwitchSlf4jToJBoss() {
        // Start with SLF4J
        LoggerFactory.setLoggingFramework(LoggingFramework.SLF4J);
        ILogger slf4jLogger = LoggerFactory.getLogger("test.switch.slf4j.jboss");
        slf4jLogger.info("Message using SLF4J");

        // Switch to JBoss Logging
        LoggerFactory.setLoggingFramework(LoggingFramework.JBOSS_LOGGING);
        ILogger jbossLogger = LoggerFactory.getLogger("test.switch.slf4j.jboss");
        jbossLogger.info("Message using JBoss Logging");

        // Verify they are different instances
        assertNotSame(
                slf4jLogger,
                jbossLogger,
                "Logger instances should be different after adapter switch");
    }

    @Test
    @Order(4)
    @DisplayName("Should switch from JBoss Logging to SLF4J")
    void testSwitchJBossToSlf4j() {
        // Start with JBoss Logging
        LoggerFactory.setLoggingFramework(LoggingFramework.JBOSS_LOGGING);
        ILogger jbossLogger = LoggerFactory.getLogger("test.switch.jboss.slf4j");
        jbossLogger.info("Message using JBoss Logging");

        // Switch to SLF4J
        LoggerFactory.setLoggingFramework(LoggingFramework.SLF4J);
        ILogger slf4jLogger = LoggerFactory.getLogger("test.switch.jboss.slf4j");
        slf4jLogger.info("Message using SLF4J");

        // Verify they are different instances
        assertNotSame(
                jbossLogger,
                slf4jLogger,
                "Logger instances should be different after adapter switch");
    }

    @Test
    @Order(5)
    @DisplayName("Should switch adapters multiple times")
    void testMultipleAdapterSwitches() {
        String loggerName = "test.multiple.switches";

        // First switch: SLF4J
        LoggerFactory.setLoggingFramework(LoggingFramework.SLF4J);
        ILogger logger1 = LoggerFactory.getLogger(loggerName);
        logger1.info("Switch 1: Using SLF4J");

        // Second switch: JBoss
        LoggerFactory.setLoggingFramework(LoggingFramework.JBOSS_LOGGING);
        ILogger logger2 = LoggerFactory.getLogger(loggerName);
        logger2.info("Switch 2: Using JBoss Logging");

        // Third switch: SLF4J again
        LoggerFactory.setLoggingFramework(LoggingFramework.SLF4J);
        ILogger logger3 = LoggerFactory.getLogger(loggerName);
        logger3.info("Switch 3: Using SLF4J again");

        // Fourth switch: JBoss again
        LoggerFactory.setLoggingFramework(LoggingFramework.JBOSS_LOGGING);
        ILogger logger4 = LoggerFactory.getLogger(loggerName);
        logger4.info("Switch 4: Using JBoss Logging again");

        // All should be different instances
        assertNotSame(logger1, logger2, "Logger 1 and 2 should be different");
        assertNotSame(logger2, logger3, "Logger 2 and 3 should be different");
        assertNotSame(logger3, logger4, "Logger 3 and 4 should be different");
    }

    @Test
    @Order(6)
    @DisplayName("Should handle formatted logging in both adapters")
    void testFormattedLoggingBothAdapters() {
        String loggerName = "test.formatted.both";

        // Test SLF4J formatted logging
        LoggerFactory.setLoggingFramework(LoggingFramework.SLF4J);
        ILogger slf4jLogger = LoggerFactory.getLogger(loggerName);
        assertDoesNotThrow(
                () -> {
                    slf4jLogger.info("SLF4J: User {} logged in from {}", "alice", "10.0.0.1");
                    slf4jLogger.debug("SLF4J: Processing {} items in {} seconds", 100, 2.5);
                },
                "SLF4J should handle formatted logging");

        // Test JBoss formatted logging
        LoggerFactory.setLoggingFramework(LoggingFramework.JBOSS_LOGGING);
        ILogger jbossLogger = LoggerFactory.getLogger(loggerName);
        assertDoesNotThrow(
                () -> {
                    jbossLogger.info("JBoss: User {} logged in from {}", "bob", "10.0.0.2");
                    jbossLogger.debug("JBoss: Processing {} items in {} seconds", 200, 3.5);
                },
                "JBoss should handle formatted logging");
    }

    @Test
    @Order(7)
    @DisplayName("Should handle exceptions in both adapters")
    void testExceptionLoggingBothAdapters() {
        String loggerName = "test.exception.both";
        Exception testException = new RuntimeException("Test exception for adapters");

        // Test SLF4J exception logging
        LoggerFactory.setLoggingFramework(LoggingFramework.SLF4J);
        ILogger slf4jLogger = LoggerFactory.getLogger(loggerName);
        assertDoesNotThrow(
                () -> slf4jLogger.error("SLF4J: An error occurred", testException),
                "SLF4J should handle exception logging");

        // Test JBoss exception logging
        LoggerFactory.setLoggingFramework(LoggingFramework.JBOSS_LOGGING);
        ILogger jbossLogger = LoggerFactory.getLogger(loggerName);
        assertDoesNotThrow(
                () -> jbossLogger.error("JBoss: An error occurred", testException),
                "JBoss should handle exception logging");
    }

    @Test
    @Order(8)
    @DisplayName("Should maintain separate logger instances per adapter")
    void testSeparateLoggerInstances() {
        // Create multiple loggers with SLF4J
        LoggerFactory.setLoggingFramework(LoggingFramework.SLF4J);
        ILogger slf4jLogger1 = LoggerFactory.getLogger("test.logger.1");
        ILogger slf4jLogger2 = LoggerFactory.getLogger("test.logger.2");

        assertNotSame(
                slf4jLogger1,
                slf4jLogger2,
                "Different logger names should create different instances");

        // Switch to JBoss
        LoggerFactory.setLoggingFramework(LoggingFramework.JBOSS_LOGGING);
        ILogger jbossLogger1 = LoggerFactory.getLogger("test.logger.1");
        ILogger jbossLogger2 = LoggerFactory.getLogger("test.logger.2");

        assertNotSame(
                jbossLogger1,
                jbossLogger2,
                "Different logger names should create different instances");

        // Verify cross-adapter differences
        assertNotSame(
                slf4jLogger1,
                jbossLogger1,
                "Same name but different adapters should be different instances");
    }

    @Test
    @Order(9)
    @DisplayName("Should check log levels in both adapters")
    void testLogLevelChecksBothAdapters() {
        String loggerName = "test.level.checks";

        // Test SLF4J level checks
        LoggerFactory.setLoggingFramework(LoggingFramework.SLF4J);
        ILogger slf4jLogger = LoggerFactory.getLogger(loggerName);
        assertDoesNotThrow(
                () -> {
                    slf4jLogger.isTraceEnabled();
                    slf4jLogger.isDebugEnabled();
                    slf4jLogger.isInfoEnabled();
                    slf4jLogger.isWarnEnabled();
                    slf4jLogger.isErrorEnabled();
                },
                "SLF4J should support level checks");

        // Test JBoss level checks
        LoggerFactory.setLoggingFramework(LoggingFramework.JBOSS_LOGGING);
        ILogger jbossLogger = LoggerFactory.getLogger(loggerName);
        assertDoesNotThrow(
                () -> {
                    jbossLogger.isTraceEnabled();
                    jbossLogger.isDebugEnabled();
                    jbossLogger.isInfoEnabled();
                    jbossLogger.isWarnEnabled();
                    jbossLogger.isErrorEnabled();
                },
                "JBoss should support level checks");
    }

    @Test
    @Order(10)
    @DisplayName("Should use generic log methods in both adapters")
    void testGenericLogMethodsBothAdapters() {
        String loggerName = "test.generic.both";

        // Test SLF4J generic methods
        LoggerFactory.setLoggingFramework(LoggingFramework.SLF4J);
        ILogger slf4jLogger = LoggerFactory.getLogger(loggerName);
        assertDoesNotThrow(
                () -> {
                    slf4jLogger.info("SLF4J generic INFO");
                    slf4jLogger.warn("SLF4J generic WARN with param: {}", "value");
                    slf4jLogger.error("SLF4J generic ERROR", new Exception("test"));
                },
                "SLF4J should support generic log methods");

        // Test JBoss generic methods
        LoggerFactory.setLoggingFramework(LoggingFramework.JBOSS_LOGGING);
        ILogger jbossLogger = LoggerFactory.getLogger(loggerName);
        assertDoesNotThrow(
                () -> {
                    jbossLogger.info("JBoss generic INFO");
                    jbossLogger.warn("JBoss generic WARN with param: {}", "value");
                    jbossLogger.error("JBoss generic ERROR", new Exception("test"));
                },
                "JBoss should support generic log methods");
    }

    @Test
    @Order(11)
    @DisplayName("Should handle concurrent adapter switches")
    void testConcurrentAdapterAccess() {
        String loggerName = "test.concurrent";

        assertDoesNotThrow(
                () -> {
                    // Rapidly switch adapters
                    for (int i = 0; i < 10; i++) {
                        LoggingFramework framework =
                                (i % 2 == 0)
                                        ? LoggingFramework.SLF4J
                                        : LoggingFramework.JBOSS_LOGGING;

                        LoggerFactory.setLoggingFramework(framework);
                        ILogger logger = LoggerFactory.getLogger(loggerName + "." + i);
                        logger.info("Message {} using {}", i, framework);
                    }
                },
                "Should handle rapid adapter switches");
    }
}
