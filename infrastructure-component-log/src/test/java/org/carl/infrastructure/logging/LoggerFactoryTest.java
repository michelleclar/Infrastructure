package org.carl.infrastructure.logging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/** Comprehensive tests for LoggerFactory Testing framework switching, caching, and configuration */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoggerFactoryTest {

    @BeforeEach
    void setUp() {
        // Reset factory before each test
        LoggerFactory.reset();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        LoggerFactory.reset();
        System.clearProperty(LoggerFactory.FRAMEWORK_PROPERTY);
    }

    @Test
    @Order(1)
    @DisplayName("Should get logger by class")
    void testGetLoggerByClass() {
        ILogger logger = LoggerFactory.getLogger(LoggerFactoryTest.class);

        assertNotNull(logger, "Logger should not be null");
        assertEquals(
                LoggerFactoryTest.class.getName(),
                logger.getName(),
                "Logger name should match class name");
    }

    @Test
    @Order(2)
    @DisplayName("Should get logger by name")
    void testGetLoggerByName() {
        String loggerName = "test.logger.custom";
        ILogger logger = LoggerFactory.getLogger(loggerName);

        assertNotNull(logger, "Logger should not be null");
        assertEquals(loggerName, logger.getName(), "Logger name should match provided name");
    }

    @Test
    @Order(3)
    @DisplayName("Should cache logger instances")
    void testLoggerCaching() {
        ILogger logger1 = LoggerFactory.getLogger("test.logger.cache");
        ILogger logger2 = LoggerFactory.getLogger("test.logger.cache");

        assertSame(logger1, logger2, "Same logger instance should be returned from cache");
    }

    @Test
    @Order(4)
    @DisplayName("Should switch to SLF4J framework")
    void testSwitchToSlf4j() {
        LoggerFactory.setLoggingFramework(LoggingFramework.SLF4J);
        assertEquals(
                LoggingFramework.SLF4J,
                LoggerFactory.getCurrentFramework(),
                "Current framework should be SLF4J");

        ILogger logger = LoggerFactory.getLogger("test.slf4j");
        assertNotNull(logger, "Logger should be created successfully");
        logger.info("Testing SLF4J framework");
    }

    @Test
    @Order(5)
    @DisplayName("Should switch to JBoss Logging framework")
    void testSwitchToJBossLogging() {
        LoggerFactory.setLoggingFramework(LoggingFramework.JBOSS_LOGGING);
        assertEquals(
                LoggingFramework.JBOSS_LOGGING,
                LoggerFactory.getCurrentFramework(),
                "Current framework should be JBoss Logging");

        ILogger logger = LoggerFactory.getLogger("test.jboss");
        assertNotNull(logger, "Logger should be created successfully");
        logger.info("Testing JBoss Logging framework");
    }

    @Test
    @Order(6)
    @DisplayName("Should auto-detect available framework")
    void testAutoDetection() {
        LoggerFactory.setLoggingFramework(LoggingFramework.AUTO);

        LoggingFramework detected = LoggerFactory.getDetectedFramework();
        assertNotNull(detected, "Should detect an available framework");
        assertTrue(
                detected == LoggingFramework.SLF4J || detected == LoggingFramework.JBOSS_LOGGING,
                "Detected framework should be either SLF4J or JBoss Logging");

        ILogger logger = LoggerFactory.getLogger("test.auto");
        assertNotNull(logger, "Logger should be created with auto-detected framework");
        logger.info("Testing auto-detected framework: {}", detected);
    }

    @Test
    @Order(7)
    @DisplayName("Should switch framework and clear cache")
    void testSwitchFrameworkClearCache() {
        // Create logger with first framework
        LoggerFactory.setLoggingFramework(LoggingFramework.SLF4J);
        ILogger logger1 = LoggerFactory.getLogger("test.switch");

        // Switch framework
        LoggerFactory.setLoggingFramework(LoggingFramework.JBOSS_LOGGING);
        ILogger logger2 = LoggerFactory.getLogger("test.switch");

        // Should be different instances after framework switch
        assertNotSame(
                logger1, logger2, "Logger instances should be different after framework switch");
    }

    @Test
    @Order(8)
    @DisplayName("Should reject null framework")
    void testSetNullFramework() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LoggerFactory.setLoggingFramework(null),
                "Should throw exception when setting null framework");
    }

    @Test
    @Order(9)
    @DisplayName("Should reset factory to initial state")
    void testReset() {
        LoggerFactory.setLoggingFramework(LoggingFramework.SLF4J);
        ILogger logger = LoggerFactory.getLogger("test.reset");
        assertNotNull(logger, "Logger should be created");

        LoggerFactory.reset();

        assertEquals(
                LoggingFramework.AUTO,
                LoggerFactory.getCurrentFramework(),
                "Framework should be reset to AUTO");
    }

    @Test
    @Order(10)
    @DisplayName("Should configure framework via system property")
    void testSystemPropertyConfiguration() {
        System.setProperty(LoggerFactory.FRAMEWORK_PROPERTY, "SLF4J");

        LoggerFactory.reset(); // Reset to pick up system property
        ILogger logger = LoggerFactory.getLogger("test.sysprop");

        assertNotNull(logger, "Logger should be created");
        logger.info("Testing system property configuration");
    }

    @Test
    @Order(11)
    @DisplayName("Should handle invalid system property gracefully")
    void testInvalidSystemProperty() {
        System.setProperty(LoggerFactory.FRAMEWORK_PROPERTY, "INVALID_FRAMEWORK");

        LoggerFactory.reset();

        // Should fallback to auto-detection
        assertDoesNotThrow(
                () -> {
                    ILogger logger = LoggerFactory.getLogger("test.invalid.sysprop");
                    assertNotNull(logger, "Logger should still be created with fallback");
                });
    }

    @Test
    @Order(12)
    @DisplayName("Should support multiple loggers simultaneously")
    void testMultipleLoggers() {
        ILogger logger1 = LoggerFactory.getLogger("test.logger1");
        ILogger logger2 = LoggerFactory.getLogger("test.logger2");
        ILogger logger3 = LoggerFactory.getLogger("test.logger3");

        assertNotNull(logger1, "Logger 1 should be created");
        assertNotNull(logger2, "Logger 2 should be created");
        assertNotNull(logger3, "Logger 3 should be created");

        assertNotSame(logger1, logger2, "Different loggers should have different instances");
        assertNotSame(logger2, logger3, "Different loggers should have different instances");

        logger1.info("Message from logger 1");
        logger2.info("Message from logger 2");
        logger3.info("Message from logger 3");
    }
}
