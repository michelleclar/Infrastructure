package org.carl.infrastructure.logging.ability;

import static org.junit.jupiter.api.Assertions.*;

import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;
import org.carl.infrastructure.logging.LoggingFramework;
import org.junit.jupiter.api.*;

/**
 * Tests for ILogAbility interface Verifies that classes implementing ILogAbility can easily get
 * logger instances
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ILogAbilityTest {

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
    @DisplayName("Should get logger from ILogAbility implementation")
    void testGetLogger() {
        TestLogService service = new TestLogService();
        ILogger logger = service.getLogger();

        assertNotNull(logger, "Logger should not be null");
        assertEquals(
                TestLogService.class.getName(),
                logger.getName(),
                "Logger name should match implementing class name");
    }

    @Test
    @Order(2)
    @DisplayName("Should log messages through ILogAbility")
    void testLogThroughAbility() {
        TestLogService service = new TestLogService();

        assertDoesNotThrow(
                () -> {
                    service.logInfo("Test info message");
                    service.logDebug("Test debug message");
                    service.logError("Test error message", new RuntimeException("Test exception"));
                },
                "Logging through ILogAbility should work");
    }

    @Test
    @Order(3)
    @DisplayName("Should work with different adapters")
    void testAbilityWithDifferentAdapters() {
        TestLogService service = new TestLogService();

        // Test with SLF4J
        LoggerFactory.setLoggingFramework(LoggingFramework.SLF4J);
        assertDoesNotThrow(
                () -> {
                    service.logInfo("SLF4J: Message through ability");
                },
                "Should work with SLF4J adapter");

        // Test with JBoss Logging
        LoggerFactory.setLoggingFramework(LoggingFramework.JBOSS_LOGGING);
        assertDoesNotThrow(
                () -> {
                    service.logInfo("JBoss: Message through ability");
                },
                "Should work with JBoss Logging adapter");
    }

    @Test
    @Order(4)
    @DisplayName("Should support conditional logging in ILogAbility")
    void testConditionalLogging() {
        TestLogService service = new TestLogService();

        assertDoesNotThrow(
                () -> {
                    service.logDebugIfEnabled("Conditional debug message");
                },
                "Conditional logging should work");
    }

    @Test
    @Order(5)
    @DisplayName("Should support all log levels through ILogAbility")
    void testAllLogLevels() {
        TestLogService service = new TestLogService();

        assertDoesNotThrow(
                () -> {
                    service.logTrace("Trace message");
                    service.logDebug("Debug message");
                    service.logInfo("Info message");
                    service.logWarn("Warn message");
                    service.logError("Error message");
                },
                "All log levels should work through ILogAbility");
    }

    @Test
    @Order(6)
    @DisplayName("Should support formatted logging through ILogAbility")
    void testFormattedLogging() {
        TestLogService service = new TestLogService();

        assertDoesNotThrow(
                () -> {
                    service.logFormatted("User {} performed action {}", "admin", "login");
                },
                "Formatted logging should work through ILogAbility");
    }

    @Test
    @Order(7)
    @DisplayName("Should support business logic with logging")
    void testBusinessLogicWithLogging() {
        OrderService orderService = new OrderService();

        assertDoesNotThrow(
                () -> {
                    orderService.processOrder("ORDER-123", 5);
                    orderService.cancelOrder("ORDER-124");
                },
                "Business logic with logging should work");
    }

    @Test
    @Order(8)
    @DisplayName("Should work with multiple ILogAbility implementations")
    void testMultipleImplementations() {
        TestLogService service1 = new TestLogService();
        OrderService service2 = new OrderService();
        UserService service3 = new UserService();

        assertDoesNotThrow(
                () -> {
                    service1.logInfo("Message from TestLogService");
                    service2.processOrder("ORD-1", 10);
                    service3.authenticateUser("user@example.com", "password");
                },
                "Multiple ILogAbility implementations should work together");
    }
}

/** Test implementation of ILogAbility */
class TestLogService implements ILogAbility {

    public void logInfo(String message) {
        getLogger().info(message);
    }

    public void logDebug(String message) {
        getLogger().debug(message);
    }

    public void logTrace(String message) {
        getLogger().trace(message);
    }

    public void logWarn(String message) {
        getLogger().warn(message);
    }

    public void logError(String message) {
        getLogger().error(message);
    }

    public void logError(String message, Throwable throwable) {
        getLogger().error(message, throwable);
    }

    public void logDebugIfEnabled(String message) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug(message);
        }
    }

    public void logFormatted(String format, Object... args) {
        getLogger().info(format, args);
    }
}

/** Order service with logging capability */
class OrderService implements ILogAbility {

    public void processOrder(String orderId, int quantity) {
        getLogger().info("Processing order: {} with quantity: {}", orderId, quantity);

        if (getLogger().isDebugEnabled()) {
            getLogger()
                    .debug(
                            "Order details - ID: {}, Quantity: {}, Time: {}",
                            orderId,
                            quantity,
                            System.currentTimeMillis());
        }

        getLogger().info("Order {} processed successfully", orderId);
    }

    public void cancelOrder(String orderId) {
        getLogger().warn("Cancelling order: {}", orderId);
        getLogger().info("Order {} cancelled", orderId);
    }
}

/** User service with logging capability */
class UserService implements ILogAbility {

    public void authenticateUser(String email, String password) {
        getLogger().info("Authenticating user: {}", email);

        if (password == null || password.isEmpty()) {
            getLogger().error("Authentication failed: empty password for user {}", email);
            return;
        }

        getLogger().debug("User {} authenticated successfully", email);
    }
}
