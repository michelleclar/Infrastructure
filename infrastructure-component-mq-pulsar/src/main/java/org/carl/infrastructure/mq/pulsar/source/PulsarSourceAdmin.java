package org.carl.infrastructure.mq.pulsar.source;

import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.client.admin.Sources;
import org.apache.pulsar.common.io.SourceConfig;
import org.carl.infrastructure.mq.common.ex.MQClientException;
import org.carl.infrastructure.mq.common.ex.SourceAdminException;
import org.carl.infrastructure.mq.config.MQConfig;
import org.carl.infrastructure.mq.pulsar.builder.PulsarAdminFactory;
import org.carl.infrastructure.mq.source.SourceAdmin;
import org.carl.infrastructure.mq.source.SourceDefinition;
import org.carl.infrastructure.mq.source.SourceInstanceReference;
import org.carl.infrastructure.mq.source.SourceInstanceStatus;
import org.carl.infrastructure.mq.source.SourcePackageLocation;
import org.carl.infrastructure.mq.source.SourceReference;
import org.carl.infrastructure.mq.source.SourceStatus;

import java.util.Objects;

public final class PulsarSourceAdmin implements SourceAdmin {

    private final PulsarSourcesClient sources;
    private final PulsarAdmin admin;
    private final boolean closeAdmin;

    public PulsarSourceAdmin(PulsarAdmin admin) {
        this(admin, false);
    }

    private PulsarSourceAdmin(PulsarAdmin admin, boolean closeAdmin) {
        this.admin = Objects.requireNonNull(admin, "admin must not be null");
        this.sources = new PulsarAdminSourcesClient(admin.sources());
        this.closeAdmin = closeAdmin;
    }

    PulsarSourceAdmin(PulsarSourcesClient sources) {
        this.sources = Objects.requireNonNull(sources, "sources must not be null");
        this.admin = null;
        this.closeAdmin = false;
    }

    public static PulsarSourceAdmin create(MQConfig.ClientConfig clientConfig) throws SourceAdminException {
        try {
            return new PulsarSourceAdmin(
                    PulsarAdminFactory.create(clientConfig)
                            .orElseThrow(
                                    () ->
                                            new SourceAdminException(
                                                    "Pulsar adminUrl is required to create SourceAdmin")),
                    true);
        } catch (MQClientException e) {
            throw new SourceAdminException(e);
        }
    }

    @Override
    public void create(SourceDefinition source) throws SourceAdminException {
        SourceConfig config = PulsarSourceConfigMapper.toPulsar(source);
        SourcePackageLocation packageLocation = source.packageLocation();
        try {
            if (packageLocation != null && packageLocation.kind() == SourcePackageLocation.Kind.URL) {
                sources.createSourceWithUrl(config, packageLocation.location());
                return;
            }
            sources.createSource(config, packageLocation == null ? null : packageLocation.location());
        } catch (PulsarAdminException e) {
            throw new SourceAdminException(e);
        }
    }

    @Override
    public void update(SourceDefinition source) throws SourceAdminException {
        SourceConfig config = PulsarSourceConfigMapper.toPulsar(source);
        SourcePackageLocation packageLocation = source.packageLocation();
        try {
            if (packageLocation != null && packageLocation.kind() == SourcePackageLocation.Kind.URL) {
                sources.updateSourceWithUrl(config, packageLocation.location());
                return;
            }
            sources.updateSource(config, packageLocation == null ? null : packageLocation.location());
        } catch (PulsarAdminException e) {
            throw new SourceAdminException(e);
        }
    }

    @Override
    public void delete(SourceReference source) throws SourceAdminException {
        try {
            sources.deleteSource(source.tenant(), source.namespace(), source.name());
        } catch (PulsarAdminException e) {
            throw new SourceAdminException(e);
        }
    }

    @Override
    public void start(SourceReference source) throws SourceAdminException {
        try {
            sources.startSource(source.tenant(), source.namespace(), source.name());
        } catch (PulsarAdminException e) {
            throw new SourceAdminException(e);
        }
    }

    @Override
    public void start(SourceInstanceReference sourceInstance) throws SourceAdminException {
        SourceReference source = sourceInstance.source();
        try {
            sources.startSource(
                    source.tenant(), source.namespace(), source.name(), sourceInstance.instanceId());
        } catch (PulsarAdminException e) {
            throw new SourceAdminException(e);
        }
    }

    @Override
    public void stop(SourceReference source) throws SourceAdminException {
        try {
            sources.stopSource(source.tenant(), source.namespace(), source.name());
        } catch (PulsarAdminException e) {
            throw new SourceAdminException(e);
        }
    }

    @Override
    public void stop(SourceInstanceReference sourceInstance) throws SourceAdminException {
        SourceReference source = sourceInstance.source();
        try {
            sources.stopSource(
                    source.tenant(), source.namespace(), source.name(), sourceInstance.instanceId());
        } catch (PulsarAdminException e) {
            throw new SourceAdminException(e);
        }
    }

    @Override
    public void restart(SourceReference source) throws SourceAdminException {
        try {
            sources.restartSource(source.tenant(), source.namespace(), source.name());
        } catch (PulsarAdminException e) {
            throw new SourceAdminException(e);
        }
    }

    @Override
    public void restart(SourceInstanceReference sourceInstance) throws SourceAdminException {
        SourceReference source = sourceInstance.source();
        try {
            sources.restartSource(
                    source.tenant(), source.namespace(), source.name(), sourceInstance.instanceId());
        } catch (PulsarAdminException e) {
            throw new SourceAdminException(e);
        }
    }

    @Override
    public SourceStatus getStatus(SourceReference source) throws SourceAdminException {
        try {
            return PulsarSourceStatusMapper.toApi(
                    sources.getSourceStatus(source.tenant(), source.namespace(), source.name()));
        } catch (PulsarAdminException e) {
            throw new SourceAdminException(e);
        }
    }

    @Override
    public SourceInstanceStatus getStatus(SourceInstanceReference sourceInstance)
            throws SourceAdminException {
        SourceReference source = sourceInstance.source();
        try {
            return PulsarSourceStatusMapper.toApi(
                    sourceInstance.instanceId(),
                    sources.getSourceStatus(
                            source.tenant(),
                            source.namespace(),
                            source.name(),
                            sourceInstance.instanceId()));
        } catch (PulsarAdminException e) {
            throw new SourceAdminException(e);
        }
    }

    @Override
    public void close() throws SourceAdminException {
        if (!closeAdmin || admin == null) {
            return;
        }
        admin.close();
    }

    private record PulsarAdminSourcesClient(Sources sources) implements PulsarSourcesClient {
        private PulsarAdminSourcesClient {
            Objects.requireNonNull(sources, "sources must not be null");
        }

        @Override
        public void createSource(SourceConfig sourceConfig, String fileName)
                throws PulsarAdminException {
            sources.createSource(sourceConfig, fileName);
        }

        @Override
        public void createSourceWithUrl(SourceConfig sourceConfig, String pkgUrl)
                throws PulsarAdminException {
            sources.createSourceWithUrl(sourceConfig, pkgUrl);
        }

        @Override
        public void updateSource(SourceConfig sourceConfig, String fileName)
                throws PulsarAdminException {
            sources.updateSource(sourceConfig, fileName);
        }

        @Override
        public void updateSourceWithUrl(SourceConfig sourceConfig, String pkgUrl)
                throws PulsarAdminException {
            sources.updateSourceWithUrl(sourceConfig, pkgUrl);
        }

        @Override
        public void deleteSource(String tenant, String namespace, String source)
                throws PulsarAdminException {
            sources.deleteSource(tenant, namespace, source);
        }

        @Override
        public void startSource(String tenant, String namespace, String source)
                throws PulsarAdminException {
            sources.startSource(tenant, namespace, source);
        }

        @Override
        public void startSource(String tenant, String namespace, String source, int instanceId)
                throws PulsarAdminException {
            sources.startSource(tenant, namespace, source, instanceId);
        }

        @Override
        public void stopSource(String tenant, String namespace, String source)
                throws PulsarAdminException {
            sources.stopSource(tenant, namespace, source);
        }

        @Override
        public void stopSource(String tenant, String namespace, String source, int instanceId)
                throws PulsarAdminException {
            sources.stopSource(tenant, namespace, source, instanceId);
        }

        @Override
        public void restartSource(String tenant, String namespace, String source)
                throws PulsarAdminException {
            sources.restartSource(tenant, namespace, source);
        }

        @Override
        public void restartSource(String tenant, String namespace, String source, int instanceId)
                throws PulsarAdminException {
            sources.restartSource(tenant, namespace, source, instanceId);
        }

        @Override
        public org.apache.pulsar.common.policies.data.SourceStatus getSourceStatus(
                String tenant, String namespace, String source)
                throws PulsarAdminException {
            return sources.getSourceStatus(tenant, namespace, source);
        }

        @Override
        public org.apache.pulsar.common.policies.data.SourceStatus.SourceInstanceStatus
                        .SourceInstanceStatusData
                getSourceStatus(String tenant, String namespace, String source, int instanceId)
                        throws PulsarAdminException {
            return sources.getSourceStatus(tenant, namespace, source, instanceId);
        }
    }
}
