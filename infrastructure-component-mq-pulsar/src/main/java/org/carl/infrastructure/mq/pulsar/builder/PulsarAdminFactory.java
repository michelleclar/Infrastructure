package org.carl.infrastructure.mq.pulsar.builder;

import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminBuilder;
import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;
import org.carl.infrastructure.mq.common.ex.MQClientException;
import org.carl.infrastructure.mq.config.MQConfig;

import java.util.Optional;

/**
 * 构建 PulsarAdmin（HTTP 管理 API），用于 Reader 的 {@code topicsPattern} 解析和 Source 生命周期管理。
 *
 * <p>认证与 TLS 配置与 {@link PulsarClientFactory} 中的 PulsarClient 保持一致。
 * 当 {@link MQConfig.ClientConfig#adminUrl()} 未配置时返回 {@link Optional#empty()}，
 * 此时使用 {@code topicsPattern} 的 Reader 会在创建阶段抛出
 * {@link org.carl.infrastructure.mq.common.ex.ReaderException}。
 */
public final class PulsarAdminFactory {

    private static final ILogger logger = LoggerFactory.getLogger(PulsarAdminFactory.class);

    private PulsarAdminFactory() {}

    /**
     * 按 {@link MQConfig.ClientConfig} 构建 PulsarAdmin；未配置 adminUrl 时返回 empty。
     *
     * @param clientConfig 客户端配置
     * @return 已构建的 PulsarAdmin，或 empty
     * @throws MQClientException 构建失败
     */
    public static Optional<PulsarAdmin> create(MQConfig.ClientConfig clientConfig) throws MQClientException {
        if (clientConfig.adminUrl().isEmpty()) {
            return Optional.empty();
        }
        String adminUrl = clientConfig.adminUrl().get();
        PulsarAdminBuilder builder = PulsarAdmin.builder().serviceHttpUrl(adminUrl);

        // 认证（与 PulsarClient 保持一致）
        if (clientConfig.authToken().isPresent()) {
            builder.authentication(AuthenticationFactory.token(clientConfig.authToken().get()));
            logger.info("PulsarAdmin token authentication configured");
        } else if (clientConfig.authPluginClassName().isPresent()) {
            try {
                builder.authentication(
                        clientConfig.authPluginClassName().get(),
                        clientConfig.authParams().orElse(""));
                logger.info(
                        "PulsarAdmin custom authentication configured: {}",
                        clientConfig.authPluginClassName().get());
            } catch (PulsarClientException.UnsupportedAuthenticationException e) {
                throw new MQClientException(e);
            }
        }

        // TLS
        if (adminUrl.startsWith("https://")) {
            builder.allowTlsInsecureConnection(clientConfig.tls().allowInsecureConnection())
                    .enableTlsHostnameVerification(clientConfig.tls().enableHostnameVerification());
            if (clientConfig.tls().trustCertsFilePath().isPresent()) {
                builder.tlsTrustCertsFilePath(clientConfig.tls().trustCertsFilePath().get());
            }
            logger.info("PulsarAdmin TLS configuration applied");
        }

        try {
            PulsarAdmin admin = builder.build();
            logger.info("PulsarAdmin created for service http url: {}", adminUrl);
            return Optional.of(admin);
        } catch (PulsarClientException e) {
            throw new MQClientException(e);
        }
    }
}
