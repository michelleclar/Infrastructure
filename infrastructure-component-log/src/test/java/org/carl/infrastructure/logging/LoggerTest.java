package org.carl.infrastructure.logging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Comprehensive tests for ILogger interface implementations Testing all log levels, formatting, and
 * exception handling
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoggerTest {

    private ILogger logger;

    @BeforeEach
    void setUp() {
        LoggerFactory.reset();
        logger = LoggerFactory.getLogger(LoggerTest.class);
    }

    @AfterEach
    void tearDown() {
        LoggerFactory.reset();
    }

    // ==================== Basic Logging Tests ====================

    @Test
    @Order(1)
    @DisplayName("Should log TRACE level messages")
    void testTraceLogging() {
        assertDoesNotThrow(
                () -> {
                    logger.trace("This is a TRACE message");
                },
                "TRACE logging should not throw exception");
    }

    @Test
    @Order(2)
    @DisplayName("Should log DEBUG level messages")
    void testDebugLogging() {
        assertDoesNotThrow(
                () -> {
                    logger.debug("This is a DEBUG message");
                },
                "DEBUG logging should not throw exception");
    }

    @Test
    @Order(3)
    @DisplayName("Should log INFO level messages")
    void testInfoLogging() {
        assertDoesNotThrow(
                () -> {
                    logger.info("This is an INFO message");
                },
                "INFO logging should not throw exception");
    }

    @Test
    @Order(4)
    @DisplayName("Should log WARN level messages")
    void testWarnLogging() {
        assertDoesNotThrow(
                () -> {
                    logger.warn("This is a WARN message");
                },
                "WARN logging should not throw exception");
    }

    @Test
    @Order(5)
    @DisplayName("Should log ERROR level messages")
    void testErrorLogging() {
        assertDoesNotThrow(
                () -> {
                    logger.error("This is an ERROR message");
                },
                "ERROR logging should not throw exception");
    }

    @Test
    @Order(6)
    @DisplayName("Should log all levels in sequence")
    void testAllLevelsSequence() {
        assertDoesNotThrow(
                () -> {
                    logger.trace("TRACE: Application starting trace");
                    logger.debug("DEBUG: Debug information");
                    logger.info("INFO: Application started successfully");
                    logger.warn("WARN: Configuration warning");
                    logger.error("ERROR: Critical error occurred");
                },
                "Logging all levels should not throw exception");
    }

    // ==================== Formatted Logging Tests ====================

    @Test
    @Order(7)
    @DisplayName("Should format TRACE messages with parameters")
    void testTraceFormatted() {
        assertDoesNotThrow(
                () -> {
                    logger.trace("TRACE: User {} logged in from {}", "john_doe", "192.168.1.1");
                });
    }

    @Test
    @Order(8)
    @DisplayName("Should format DEBUG messages with parameters")
    void testDebugFormatted() {
        assertDoesNotThrow(
                () -> {
                    logger.debug("DEBUG: Processing request {} with {} items", 12345, 10);
                });
    }

    @Test
    @Order(9)
    @DisplayName("Should format INFO messages with parameters")
    void testInfoFormatted() {
        assertDoesNotThrow(
                () -> {
                    logger.info("INFO: Order {} completed in {} ms", "ORD-123", 150);
                });
    }

    @Test
    @Order(10)
    @DisplayName("Should format WARN messages with parameters")
    void testWarnFormatted() {
        assertDoesNotThrow(
                () -> {
                    logger.warn("WARN: Cache size {} exceeds limit {}", 1000, 500);
                });
    }

    @Test
    @Order(11)
    @DisplayName("Should format ERROR messages with parameters")
    void testErrorFormatted() {
        assertDoesNotThrow(
                () -> {
                    logger.error("ERROR: Failed to connect to {} after {} attempts", "database", 3);
                });
    }

    @Test
    @Order(12)
    @DisplayName("Should handle multiple parameters")
    void testMultipleParameters() {
        assertDoesNotThrow(
                () -> {
                    logger.info(
                            "Processing: user={}, action={}, resource={}, time={}",
                            "admin",
                            "update",
                            "config.xml",
                            System.currentTimeMillis());
                });
    }

    // ==================== Exception Logging Tests ====================

    @Test
    @Order(13)
    @DisplayName("Should log TRACE with exception")
    void testTraceWithException() {
        Exception ex = new RuntimeException("Test trace exception");
        assertDoesNotThrow(
                () -> {
                    logger.trace("TRACE message with exception", ex);
                });
    }

    @Test
    @Order(14)
    @DisplayName("Should log DEBUG with exception")
    void testDebugWithException() {
        Exception ex = new IllegalArgumentException("Invalid argument");
        assertDoesNotThrow(
                () -> {
                    logger.debug("DEBUG: Validation failed", ex);
                });
    }

    @Test
    @Order(15)
    @DisplayName("Should log INFO with exception")
    void testInfoWithException() {
        Exception ex = new IllegalStateException("Invalid state");
        assertDoesNotThrow(
                () -> {
                    logger.info("INFO: State change error", ex);
                });
    }

    @Test
    @Order(16)
    @DisplayName("Should log WARN with exception")
    void testWarnWithException() {
        Exception ex = new InterruptedException("Operation interrupted");
        assertDoesNotThrow(
                () -> {
                    logger.warn("WARN: Operation was interrupted", ex);
                });
    }

    @Test
    @Order(17)
    @DisplayName("Should log ERROR with exception")
    void testErrorWithException() {
        Exception ex = new NullPointerException("Null value encountered");
        assertDoesNotThrow(
                () -> {
                    logger.error("ERROR: Null pointer error", ex);
                });
    }

    @Test
    @Order(18)
    @DisplayName("Should log nested exceptions")
    void testNestedExceptions() {
        Exception cause = new IllegalArgumentException("Root cause");
        Exception wrapper = new RuntimeException("Wrapper exception", cause);

        assertDoesNotThrow(
                () -> {
                    logger.error("ERROR: Nested exception occurred", wrapper);
                });
    }

    // ==================== Generic Log Methods Tests ====================

    @Test
    @Order(19)
    @DisplayName("Should log using generic method with all levels")
    void testGenericLogMethods() {
        assertDoesNotThrow(
                () -> {
                    logger.trace("Generic TRACE message");
                    logger.debug("Generic DEBUG message");
                    logger.info("Generic INFO message");
                    logger.warn("Generic WARN message");
                    logger.error("Generic ERROR message");
                });
    }

    @Test
    @Order(20)
    @DisplayName("Should format messages using generic method")
    void testGenericLogWithFormatting() {
        assertDoesNotThrow(
                () -> {
                    logger.info("Generic formatted: {} = {}", "key", "value");
                    logger.warn("Warning: {} items exceeding limit {}", 100, 50);
                });
    }

    @Test
    @Order(21)
    @DisplayName("Should log exceptions using generic method")
    void testGenericLogWithException() {
        Exception ex = new RuntimeException("Generic exception test");
        assertDoesNotThrow(
                () -> {
                    logger.error("Generic error with exception", ex);
                });
    }

    // ==================== Level Check Tests ====================

    @Test
    @Order(22)
    @DisplayName("Should check if TRACE is enabled")
    void testIsTraceEnabled() {
        assertDoesNotThrow(
                () -> {
                    boolean enabled = logger.isTraceEnabled();
                    // Just verify the method works, actual value depends on configuration
                });
    }

    @Test
    @Order(23)
    @DisplayName("Should check if DEBUG is enabled")
    void testIsDebugEnabled() {
        assertDoesNotThrow(
                () -> {
                    boolean enabled = logger.isDebugEnabled();
                });
    }

    @Test
    @Order(24)
    @DisplayName("Should check if INFO is enabled")
    void testIsInfoEnabled() {
        assertDoesNotThrow(
                () -> {
                    boolean enabled = logger.isInfoEnabled();
                    assertTrue(enabled, "INFO level should typically be enabled");
                });
    }

    @Test
    @Order(25)
    @DisplayName("Should check if WARN is enabled")
    void testIsWarnEnabled() {
        assertDoesNotThrow(
                () -> {
                    boolean enabled = logger.isWarnEnabled();
                    assertTrue(enabled, "WARN level should typically be enabled");
                });
    }

    @Test
    @Order(26)
    @DisplayName("Should check if ERROR is enabled")
    void testIsErrorEnabled() {
        assertDoesNotThrow(
                () -> {
                    boolean enabled = logger.isErrorEnabled();
                    assertTrue(enabled, "ERROR level should typically be enabled");
                });
    }

    @Test
    @Order(27)
    @DisplayName("Should check log level using generic method")
    void testIsEnabledWithLevel() {
        assertDoesNotThrow(
                () -> {
                    logger.isEnabled(LogLevel.TRACE);
                    logger.isEnabled(LogLevel.DEBUG);
                    logger.isEnabled(LogLevel.INFO);
                    logger.isEnabled(LogLevel.WARN);
                    logger.isEnabled(LogLevel.ERROR);
                });
    }

    // ==================== Logger Name Tests ====================

    @Test
    @Order(28)
    @DisplayName("Should return correct logger name")
    void testLoggerName() {
        assertNotNull(logger.getName(), "Logger name should not be null");
        assertEquals(
                LoggerTest.class.getName(),
                logger.getName(),
                "Logger name should match test class name");
    }

    // ==================== Performance Tests ====================

    @Test
    @Order(29)
    @DisplayName("Should efficiently skip disabled log levels")
    void testConditionalLogging() {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            if (logger.isDebugEnabled()) {
                logger.debug("Debug message {}", i);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        // This should be fast if debug is disabled
        assertTrue(duration < 1000, "Conditional logging should be efficient");
    }

    // ==================== Edge Cases ====================

    @Test
    @Order(30)
    @DisplayName("Should handle null message gracefully")
    void testNullMessage() {
        assertDoesNotThrow(
                () -> {
                    logger.info(null);
                });
    }

    @Test
    @Order(31)
    @DisplayName("Should handle empty message")
    void testEmptyMessage() {
        assertDoesNotThrow(
                () -> {
                    logger.info("");
                });
    }

    @Test
    @Order(32)
    @DisplayName("Should handle null parameters in formatting")
    void testNullParameters() {
        assertDoesNotThrow(
                () -> {
                    logger.info("Message with null: {}", (Object) null);
                });
    }

    @Test
    @Order(33)
    @DisplayName("Should handle special characters in message")
    void testSpecialCharacters() {
        assertDoesNotThrow(
                () -> {
                    logger.info("Special chars: {} [] () <> | & $ @ # %");
                    logger.info("Unicode: 你好世界 こんにちは مرحبا");
                });
    }
}
