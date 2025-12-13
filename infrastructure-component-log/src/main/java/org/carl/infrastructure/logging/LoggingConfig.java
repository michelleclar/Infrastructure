package org.carl.infrastructure.logging;

/** Logging configuration holder. Provides centralized configuration for the logging module. */
public class LoggingConfig {

    private LoggingFramework framework = LoggingFramework.AUTO;

    /** Default constructor */
    public LoggingConfig() {}

    /**
     * Constructor with framework
     *
     * @param framework the logging framework
     */
    public LoggingConfig(LoggingFramework framework) {
        this.framework = framework;
    }

    /**
     * Get the logging framework
     *
     * @return the logging framework
     */
    public LoggingFramework getFramework() {
        return framework;
    }

    /**
     * Set the logging framework
     *
     * @param framework the logging framework
     * @return this config instance for method chaining
     */
    public LoggingConfig setFramework(LoggingFramework framework) {
        this.framework = framework;
        return this;
    }

    /** Apply this configuration to the LoggerFactory */
    public void apply() {
        LoggerFactory.setLoggingFramework(this.framework);
    }

    /**
     * Create a builder for logging configuration
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for LoggingConfig */
    public static class Builder {
        private LoggingFramework framework = LoggingFramework.AUTO;

        /**
         * Set the logging framework
         *
         * @param framework the logging framework
         * @return this builder
         */
        public Builder framework(LoggingFramework framework) {
            this.framework = framework;
            return this;
        }

        /**
         * Use SLF4J as the logging framework
         *
         * @return this builder
         */
        public Builder useSlf4j() {
            this.framework = LoggingFramework.SLF4J;
            return this;
        }

        /**
         * Use JBoss Logging as the logging framework
         *
         * @return this builder
         */
        public Builder useJBossLogging() {
            this.framework = LoggingFramework.JBOSS_LOGGING;
            return this;
        }

        /**
         * Use auto-detection for the logging framework
         *
         * @return this builder
         */
        public Builder autoDetect() {
            this.framework = LoggingFramework.AUTO;
            return this;
        }

        /**
         * Build the configuration
         *
         * @return the logging configuration
         */
        public LoggingConfig build() {
            return new LoggingConfig(framework);
        }

        /** Build and apply the configuration */
        public void buildAndApply() {
            build().apply();
        }
    }
}
