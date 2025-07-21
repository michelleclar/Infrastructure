package org.carl.infrastructure.pulsar.config;

import io.opentelemetry.api.OpenTelemetry;

import org.apache.pulsar.client.api.*;
import org.jboss.logging.Logger;

import java.util.concurrent.TimeUnit;

public class PulsarClientFactory {
    private final ClientBuilder clientBuilder;

    public static PulsarClientFactory getInstance() {
        return new PulsarClientFactory();
    }

    public PulsarClientFactory() {
        this.clientBuilder = PulsarClient.builder();
    }

    public PulsarClientProcessConnect processConnect(MsgArgsConfig.ClientConfig clientConfig) {
        return new PulsarClientProcessConnect(clientBuilder, clientConfig);
    }

    public static class PulsarClientProcessConnect {
        private final ClientBuilder clientBuilder;
        private final MsgArgsConfig.ClientConfig clientConfig;

        private static final Logger logger = Logger.getLogger(PulsarClientProcessConnect.class);

        public PulsarClientProcessConnect(
                ClientBuilder clientBuilder, MsgArgsConfig.ClientConfig clientConfig) {
            this.clientBuilder = clientBuilder;
            this.clientConfig = clientConfig;
        }

        public PulsarClientProcessTx process(MsgArgsConfig.TransactionConfig transactionConfig)
                throws PulsarClientException.UnsupportedAuthenticationException {
            logger.debugf("Creating Pulsar client with args: {%s}", clientConfig);

            this.clientBuilder
                    .serviceUrl(clientConfig.serviceUrl())
                    .operationTimeout(
                            (int) clientConfig.operationTimeout().toSeconds(), TimeUnit.SECONDS)
                    .connectionTimeout(
                            (int) clientConfig.connectionTimeout().toSeconds(), TimeUnit.SECONDS)
                    .connectionsPerBroker(clientConfig.connectionsPerBroker())
                    .enableTcpNoDelay(clientConfig.tcpNoDelay())
                    .keepAliveInterval(
                            (int) clientConfig.keepAliveInterval().toSeconds(), TimeUnit.SECONDS)
                    .memoryLimit(clientConfig.memoryLimit(), SizeUnit.BYTES)
                    .maxLookupRequests(clientConfig.maxLookupRequests())
                    .maxLookupRedirects(clientConfig.maxLookupRedirects())
                    .maxConcurrentLookupRequests(clientConfig.maxConcurrentLookupRequests());

            // 配置认证
            if (clientConfig.authToken().isPresent()) {
                this.clientBuilder.authentication(
                        AuthenticationFactory.token(clientConfig.authToken().get()));
                logger.info("Token authentication configured");
            } else if (clientConfig.authPluginClassName().isPresent()) {
                this.clientBuilder.authentication(
                        clientConfig.authPluginClassName().get(),
                        clientConfig.authParams().orElse(""));
                logger.infof(
                        "Custom authentication configured: %s",
                        clientConfig.authPluginClassName().get());
            }
            // 配置 TLS
            if (clientConfig.serviceUrl().startsWith("pulsar+ssl://")) {
                this.clientBuilder
                        .allowTlsInsecureConnection(clientConfig.tls().allowInsecureConnection())
                        .enableTlsHostnameVerification(
                                clientConfig.tls().enableHostnameVerification());

                if (clientConfig.tls().trustCertsFilePath().isPresent()) {
                    this.clientBuilder.tlsTrustCertsFilePath(
                            clientConfig.tls().trustCertsFilePath().get());
                }
                logger.info("TLS configuration applied");
            }

            return new PulsarClientProcessTx(this.clientBuilder, transactionConfig);
        }
    }

    public static class PulsarClientProcessTx {
        private final ClientBuilder clientBuilder;
        private final MsgArgsConfig.TransactionConfig transactionConfig;
        private static final Logger logger = Logger.getLogger(PulsarClientProcessTx.class);

        public PulsarClientProcessTx(
                ClientBuilder clientBuilder, MsgArgsConfig.TransactionConfig transactionConfig) {
            this.clientBuilder = clientBuilder;
            this.transactionConfig = transactionConfig;
        }

        public PulsarClientProcessMetrics process(MsgArgsConfig.MonitoringConfig monitoringConfig) {

            // 配置事务
            if (this.transactionConfig.enabled()) {
                this.clientBuilder.enableTransaction(true);
                logger.info("Transaction support enabled");
            }

            return new PulsarClientProcessMetrics(this.clientBuilder, monitoringConfig);
        }
    }

    public static class PulsarClientProcessMetrics {
        private final ClientBuilder clientBuilder;
        private final MsgArgsConfig.MonitoringConfig monitoringConfig;
        private static final Logger logger = Logger.getLogger(PulsarClientProcessMetrics.class);

        public PulsarClientProcessMetrics(
                ClientBuilder clientBuilder, MsgArgsConfig.MonitoringConfig monitoringConfig) {
            this.clientBuilder = clientBuilder;
            this.monitoringConfig = monitoringConfig;
        }

        public ClientBuilder build() {

            // 配置监控
            if (monitoringConfig.metricsEnabled()) {
                this.clientBuilder.openTelemetry(OpenTelemetry.noop());
                logger.info("Metrics collection enabled with interval");
            }
            return clientBuilder;
        }
    }
}
