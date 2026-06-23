package org.carl.infrastructure.mq.pulsar.source;

import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.common.io.SourceConfig;

interface PulsarSourcesClient {
    void createSource(SourceConfig sourceConfig, String fileName) throws PulsarAdminException;

    void createSourceWithUrl(SourceConfig sourceConfig, String pkgUrl) throws PulsarAdminException;

    void updateSource(SourceConfig sourceConfig, String fileName) throws PulsarAdminException;

    void updateSourceWithUrl(SourceConfig sourceConfig, String pkgUrl) throws PulsarAdminException;

    void deleteSource(String tenant, String namespace, String source) throws PulsarAdminException;

    void startSource(String tenant, String namespace, String source) throws PulsarAdminException;

    void startSource(String tenant, String namespace, String source, int instanceId)
            throws PulsarAdminException;

    void stopSource(String tenant, String namespace, String source) throws PulsarAdminException;

    void stopSource(String tenant, String namespace, String source, int instanceId)
            throws PulsarAdminException;

    void restartSource(String tenant, String namespace, String source) throws PulsarAdminException;

    void restartSource(String tenant, String namespace, String source, int instanceId)
            throws PulsarAdminException;

    org.apache.pulsar.common.policies.data.SourceStatus getSourceStatus(
            String tenant, String namespace, String source)
            throws PulsarAdminException;

    org.apache.pulsar.common.policies.data.SourceStatus.SourceInstanceStatus.SourceInstanceStatusData
            getSourceStatus(String tenant, String namespace, String source, int instanceId)
                    throws PulsarAdminException;
}
