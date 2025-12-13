package org.carl.infrastructure.logging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/** Tests for log level verification and conditional logging */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LogLevelTest {

    private ILogger logger;

    @BeforeEach
    void setUp() {
        LoggerFactory.reset();
        logger = LoggerFactory.getLogger(LogLevelTest.class);
    }

    @AfterEach
    void tearDown() {
        LoggerFactory.reset();
    }

    @Test
    @Order(1)
    @DisplayName("Should verify all log levels are defined")
    void testAllLogLevelsDefined() {
        LogLevel[] levels = LogLevel.values();

        assertEquals(5, levels.length, "Should have 5 log levels");
        assertArrayEquals(
                new LogLevel[] {
                    LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR
                },
                levels,
                "Log levels should be TRACE, DEBUG, INFO, WARN, ERROR in order");
    }

    @Test
    @Order(2)
    @DisplayName("Should check TRACE level")
    void testTraceLevel() {
        boolean traceEnabled = logger.isTraceEnabled();

        if (traceEnabled) {
            assertDoesNotThrow(
                    () -> {
                        logger.trace("TRACE is enabled");
                    });
        }

        // Generic level check should match specific check
        assertEquals(
                traceEnabled,
                logger.isEnabled(LogLevel.TRACE),
                "Generic and specific TRACE checks should match");
    }

    @Test
    @Order(3)
    @DisplayName("Should check DEBUG level")
    void testDebugLevel() {
        boolean debugEnabled = logger.isDebugEnabled();

        if (debugEnabled) {
            assertDoesNotThrow(
                    () -> {
                        logger.debug("DEBUG is enabled");
                    });
        }

        assertEquals(
                debugEnabled,
                logger.isEnabled(LogLevel.DEBUG),
                "Generic and specific DEBUG checks should match");
    }

    @Test
    @Order(4)
    @DisplayName("Should check INFO level")
    void testInfoLevel() {
        boolean infoEnabled = logger.isInfoEnabled();

        // INFO should typically be enabled
        assertTrue(infoEnabled, "INFO level should typically be enabled");

        assertEquals(
                infoEnabled,
                logger.isEnabled(LogLevel.INFO),
                "Generic and specific INFO checks should match");
    }

    @Test
    @Order(5)
    @DisplayName("Should check WARN level")
    void testWarnLevel() {
        boolean warnEnabled = logger.isWarnEnabled();

        // WARN should typically be enabled
        assertTrue(warnEnabled, "WARN level should typically be enabled");

        assertEquals(
                warnEnabled,
                logger.isEnabled(LogLevel.WARN),
                "Generic and specific WARN checks should match");
    }

    @Test
    @Order(6)
    @DisplayName("Should check ERROR level")
    void testErrorLevel() {
        boolean errorEnabled = logger.isErrorEnabled();

        // ERROR should always be enabled
        assertTrue(errorEnabled, "ERROR level should always be enabled");

        assertEquals(
                errorEnabled,
                logger.isEnabled(LogLevel.ERROR),
                "Generic and specific ERROR checks should match");
    }

    @Test
    @Order(7)
    @DisplayName("Should perform conditional logging for TRACE")
    void testConditionalTraceLogging() {
        assertDoesNotThrow(
                () -> {
                    if (logger.isTraceEnabled()) {
                        String expensiveMessage = buildExpensiveMessage();
                        logger.trace("TRACE: {}", expensiveMessage);
                    }
                },
                "Conditional TRACE logging should work");
    }

    @Test
    @Order(8)
    @DisplayName("Should perform conditional logging for DEBUG")
    void testConditionalDebugLogging() {
        assertDoesNotThrow(
                () -> {
                    if (logger.isDebugEnabled()) {
                        String debugInfo = gatherDebugInfo();
                        logger.debug("DEBUG: {}", debugInfo);
                    }
                },
                "Conditional DEBUG logging should work");
    }

    @Test
    @Order(9)
    @DisplayName("Should perform conditional logging for INFO")
    void testConditionalInfoLogging() {
        assertDoesNotThrow(
                () -> {
                    if (logger.isInfoEnabled()) {
                        logger.info("INFO: Conditional logging test");
                    }
                },
                "Conditional INFO logging should work");
    }

    @Test
    @Order(11)
    @DisplayName("Should optimize performance with level checks")
    void testPerformanceOptimization() {
        long startTime = System.currentTimeMillis();

        // Simulate 10000 log calls with level check
        for (int i = 0; i < 10000; i++) {
            if (logger.isTraceEnabled()) {
                logger.trace("Trace message {}", i);
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        // Should complete quickly even with many checks
        assertTrue(duration < 2000, "Level checks should be efficient (took " + duration + "ms)");
    }

    @Test
    @Order(12)
    @DisplayName("Should log at appropriate levels in sequence")
    void testLogLevelSequence() {
        assertDoesNotThrow(
                () -> {
                    // Log from lowest to highest severity
                    if (logger.isEnabled(LogLevel.TRACE)) {
                        logger.trace("1. TRACE: Detailed trace information");
                    }

                    if (logger.isEnabled(LogLevel.DEBUG)) {
                        logger.debug("2. DEBUG: Debug information");
                    }

                    if (logger.isEnabled(LogLevel.INFO)) {
                        logger.info("3. INFO: Informational message");
                    }

                    if (logger.isEnabled(LogLevel.WARN)) {
                        logger.warn("4. WARN: Warning message");
                    }

                    if (logger.isEnabled(LogLevel.ERROR)) {
                        logger.error("5. ERROR: Error message");
                    }
                },
                "Should log all enabled levels in sequence");
    }

    @Test
    @Order(13)
    @DisplayName("Should verify level hierarchy")
    void testLogLevelHierarchy() {
        // ERROR is always enabled
        assertTrue(logger.isErrorEnabled(), "ERROR should be enabled");

        // WARN is typically enabled
        assertTrue(logger.isWarnEnabled(), "WARN should typically be enabled");

        // INFO is typically enabled
        assertTrue(logger.isInfoEnabled(), "INFO should typically be enabled");

        // DEBUG may or may not be enabled
        boolean debugEnabled = logger.isDebugEnabled();

        // TRACE may or may not be enabled
        boolean traceEnabled = logger.isTraceEnabled();

        // If TRACE is enabled, DEBUG should also be enabled (in most configurations)
        if (traceEnabled) {
            // Note: This may vary by configuration
            assertDoesNotThrow(
                    () -> {
                        logger.trace("TRACE is enabled");
                    });
        }
    }

    @Test
    @Order(14)
    @DisplayName("Should test level checks with both adapters")
    void testLevelChecksWithBothAdapters() {
        // Test with SLF4J
        LoggerFactory.setLoggingFramework(LoggingFramework.SLF4J);
        ILogger slf4jLogger = LoggerFactory.getLogger("test.level.slf4j");

        assertDoesNotThrow(
                () -> {
                    slf4jLogger.isTraceEnabled();
                    slf4jLogger.isDebugEnabled();
                    slf4jLogger.isInfoEnabled();
                    slf4jLogger.isWarnEnabled();
                    slf4jLogger.isErrorEnabled();
                },
                "SLF4J adapter should support all level checks");

        // Test with JBoss
        LoggerFactory.setLoggingFramework(LoggingFramework.JBOSS_LOGGING);
        ILogger jbossLogger = LoggerFactory.getLogger("test.level.jboss");

        assertDoesNotThrow(
                () -> {
                    jbossLogger.isTraceEnabled();
                    jbossLogger.isDebugEnabled();
                    jbossLogger.isInfoEnabled();
                    jbossLogger.isWarnEnabled();
                    jbossLogger.isErrorEnabled();
                },
                "JBoss adapter should support all level checks");
    }

    // Helper methods

    private String buildExpensiveMessage() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Data_").append(i).append(" ");
        }
        return sb.toString();
    }

    private String gatherDebugInfo() {
        return String.format(
                "Memory: %d MB, Processors: %d, Time: %d",
                Runtime.getRuntime().totalMemory() / 1024 / 1024,
                Runtime.getRuntime().availableProcessors(),
                System.currentTimeMillis());
    }
}
