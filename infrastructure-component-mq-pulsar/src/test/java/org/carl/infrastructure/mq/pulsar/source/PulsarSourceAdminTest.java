package org.carl.infrastructure.mq.pulsar.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.common.io.SourceConfig;
import org.apache.pulsar.common.policies.data.ExceptionInformation;
import org.carl.infrastructure.mq.common.ex.SourceAdminException;
import org.carl.infrastructure.mq.source.SourceDefinition;
import org.carl.infrastructure.mq.source.SourceInstanceReference;
import org.carl.infrastructure.mq.source.SourceInstanceStatus;
import org.carl.infrastructure.mq.source.SourcePackageLocation;
import org.carl.infrastructure.mq.source.SourceProcessingGuarantees;
import org.carl.infrastructure.mq.source.SourceProducerDefinition;
import org.carl.infrastructure.mq.source.SourceReference;
import org.carl.infrastructure.mq.source.SourceResources;
import org.carl.infrastructure.mq.source.SourceStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class PulsarSourceAdminTest {

    @Test
    void createWithFileBuildsPulsarSourceConfig() throws SourceAdminException {
        FakePulsarSourcesClient sources = new FakePulsarSourcesClient();
        PulsarSourceAdmin admin = new PulsarSourceAdmin(sources);
        Map<String, Object> configs = new LinkedHashMap<>();
        configs.put("database.hostname", "postgres.example");
        configs.put("database.port", 5432);
        Map<String, Object> secrets = new LinkedHashMap<>();
        secrets.put("database.password", "secretRef");
        SourceDefinition source =
                SourceDefinition.builder("public", "default", "inventory-source")
                        .className("io.debezium.connector.postgresql.PostgresConnector")
                        .topicName("persistent://public/default/inventory")
                        .producer(
                                new SourceProducerDefinition(
                                        1000, 5000, true, "KEY_BASED", "ZSTD"))
                        .serdeClassName("org.apache.pulsar.functions.api.utils.DefaultSerDe")
                        .schemaType("JSON")
                        .configs(configs)
                        .secrets(secrets)
                        .parallelism(2)
                        .processingGuarantees(SourceProcessingGuarantees.ATLEAST_ONCE)
                        .resources(new SourceResources(0.5d, 268435456L, 1073741824L))
                        .sourceType("debezium-postgres")
                        .archive("builtin://debezium-postgres")
                        .runtimeFlags("-Dfile.encoding=UTF-8")
                        .customRuntimeOptions("{\"nodeSelector\":{\"role\":\"cdc\"}}")
                        .batchBuilder("KEY_BASED")
                        .logTopic("persistent://public/default/source-log")
                        .packageFile("/opt/connectors/debezium-postgres.nar")
                        .build();

        admin.create(source);

        assertEquals("createSource", sources.lastMethod);
        assertEquals("/opt/connectors/debezium-postgres.nar", sources.lastPackage);
        SourceConfig config = sources.lastConfig;
        assertEquals("public", config.getTenant());
        assertEquals("default", config.getNamespace());
        assertEquals("inventory-source", config.getName());
        assertEquals("io.debezium.connector.postgresql.PostgresConnector", config.getClassName());
        assertEquals("persistent://public/default/inventory", config.getTopicName());
        assertEquals("org.apache.pulsar.functions.api.utils.DefaultSerDe", config.getSerdeClassName());
        assertEquals("JSON", config.getSchemaType());
        assertEquals(configs, config.getConfigs());
        assertEquals(secrets, config.getSecrets());
        assertEquals(2, config.getParallelism());
        assertEquals(
                org.apache.pulsar.common.functions.FunctionConfig.ProcessingGuarantees.ATLEAST_ONCE,
                config.getProcessingGuarantees());
        assertEquals(0.5d, config.getResources().getCpu());
        assertEquals(268435456L, config.getResources().getRam());
        assertEquals(1073741824L, config.getResources().getDisk());
        assertEquals("debezium-postgres", config.getSourceType());
        assertEquals("builtin://debezium-postgres", config.getArchive());
        assertEquals("-Dfile.encoding=UTF-8", config.getRuntimeFlags());
        assertEquals("{\"nodeSelector\":{\"role\":\"cdc\"}}", config.getCustomRuntimeOptions());
        assertEquals("KEY_BASED", config.getBatchBuilder());
        assertEquals("persistent://public/default/source-log", config.getLogTopic());
        assertNotNull(config.getProducerConfig());
        assertEquals(1000, config.getProducerConfig().getMaxPendingMessages());
        assertEquals(5000, config.getProducerConfig().getMaxPendingMessagesAcrossPartitions());
        assertEquals(true, config.getProducerConfig().getUseThreadLocalProducers());
        assertEquals("KEY_BASED", config.getProducerConfig().getBatchBuilder());
        assertEquals(
                org.apache.pulsar.client.api.CompressionType.ZSTD,
                config.getProducerConfig().getCompressionType());
    }

    @Test
    void updateWithUrlBuildsPulsarSourceConfig() throws SourceAdminException {
        FakePulsarSourcesClient sources = new FakePulsarSourcesClient();
        PulsarSourceAdmin admin = new PulsarSourceAdmin(sources);
        SourceDefinition source =
                SourceDefinition.builder("public", "default", "mysql-source")
                        .sourceType("debezium-mysql")
                        .packageLocation(SourcePackageLocation.url("http://repo.example/source.nar"))
                        .build();

        admin.update(source);

        assertEquals("updateSourceWithUrl", sources.lastMethod);
        assertEquals("http://repo.example/source.nar", sources.lastPackage);
        assertEquals("public", sources.lastConfig.getTenant());
        assertEquals("default", sources.lastConfig.getNamespace());
        assertEquals("mysql-source", sources.lastConfig.getName());
        assertEquals("debezium-mysql", sources.lastConfig.getSourceType());
    }

    @Test
    void lifecycleOperationsUseSourceReference() throws SourceAdminException {
        FakePulsarSourcesClient sources = new FakePulsarSourcesClient();
        PulsarSourceAdmin admin = new PulsarSourceAdmin(sources);
        SourceReference source = new SourceReference("tenant-a", "namespace-a", "source-a");
        SourceInstanceReference instance = new SourceInstanceReference(source, 1);

        admin.delete(source);
        admin.start(source);
        admin.start(instance);
        admin.stop(source);
        admin.stop(instance);
        admin.restart(source);
        admin.restart(instance);

        assertEquals(
                List.of(
                        "deleteSource:tenant-a/namespace-a/source-a",
                        "startSource:tenant-a/namespace-a/source-a",
                        "startSource:tenant-a/namespace-a/source-a/1",
                        "stopSource:tenant-a/namespace-a/source-a",
                        "stopSource:tenant-a/namespace-a/source-a/1",
                        "restartSource:tenant-a/namespace-a/source-a",
                        "restartSource:tenant-a/namespace-a/source-a/1"),
                sources.calls);
    }

    @Test
    void getStatusMapsPulsarStatus() throws SourceAdminException {
        FakePulsarSourcesClient sources = new FakePulsarSourcesClient();
        sources.status = new org.apache.pulsar.common.policies.data.SourceStatus();
        sources.status.setNumInstances(2);
        sources.status.setNumRunning(1);
        var instance = new org.apache.pulsar.common.policies.data.SourceStatus.SourceInstanceStatus();
        instance.setInstanceId(0);
        instance.setStatus(statusData());
        sources.status.setInstances(List.of(instance));
        PulsarSourceAdmin admin = new PulsarSourceAdmin(sources);

        SourceStatus status = admin.getStatus(new SourceReference("tenant-a", "namespace-a", "source-a"));

        assertEquals(2, status.numInstances());
        assertEquals(1, status.numRunning());
        assertEquals(1, status.instances().size());
        SourceInstanceStatus instanceStatus = status.instances().getFirst();
        assertEquals(0, instanceStatus.instanceId());
        assertEquals(true, instanceStatus.running());
        assertEquals("worker-a", instanceStatus.workerId());
        assertEquals(9, instanceStatus.numWritten());
        assertEquals(1, instanceStatus.latestSystemExceptions().size());
        assertEquals("system-ex", instanceStatus.latestSystemExceptions().getFirst().exceptionString());
    }

    @Test
    void getInstanceStatusMapsPulsarInstanceStatus() throws SourceAdminException {
        FakePulsarSourcesClient sources = new FakePulsarSourcesClient();
        sources.instanceStatus = statusData();
        PulsarSourceAdmin admin = new PulsarSourceAdmin(sources);

        SourceInstanceStatus status =
                admin.getStatus(
                        new SourceInstanceReference("tenant-a", "namespace-a", "source-a", 3));

        assertEquals(3, status.instanceId());
        assertEquals(true, status.running());
        assertEquals("source-ex", status.latestSourceExceptions().getFirst().exceptionString());
    }

    private static org.apache.pulsar.common.policies.data.SourceStatus.SourceInstanceStatus
                    .SourceInstanceStatusData
            statusData() {
        var data =
                new org.apache.pulsar.common.policies.data.SourceStatus.SourceInstanceStatus
                        .SourceInstanceStatusData();
        data.setRunning(true);
        data.setError(null);
        data.setNumRestarts(2);
        data.setNumReceivedFromSource(7);
        data.setNumSystemExceptions(1);
        data.setLatestSystemExceptions(List.of(exception("system-ex", 10L)));
        data.setNumSourceExceptions(1);
        data.setLatestSourceExceptions(List.of(exception("source-ex", 11L)));
        data.setNumWritten(9);
        data.setLastReceivedTime(12L);
        data.setWorkerId("worker-a");
        return data;
    }

    private static ExceptionInformation exception(String text, long timestampMs) {
        ExceptionInformation exception = new ExceptionInformation();
        exception.setExceptionString(text);
        exception.setTimestampMs(timestampMs);
        return exception;
    }

    private static final class FakePulsarSourcesClient implements PulsarSourcesClient {
        private final List<String> calls = new ArrayList<>();
        private String lastMethod;
        private SourceConfig lastConfig;
        private String lastPackage;
        private org.apache.pulsar.common.policies.data.SourceStatus status;
        private org.apache.pulsar.common.policies.data.SourceStatus.SourceInstanceStatus
                        .SourceInstanceStatusData
                instanceStatus;

        @Override
        public void createSource(SourceConfig sourceConfig, String fileName) {
            lastMethod = "createSource";
            lastConfig = sourceConfig;
            lastPackage = fileName;
        }

        @Override
        public void createSourceWithUrl(SourceConfig sourceConfig, String pkgUrl) {
            lastMethod = "createSourceWithUrl";
            lastConfig = sourceConfig;
            lastPackage = pkgUrl;
        }

        @Override
        public void updateSource(SourceConfig sourceConfig, String fileName) {
            lastMethod = "updateSource";
            lastConfig = sourceConfig;
            lastPackage = fileName;
        }

        @Override
        public void updateSourceWithUrl(SourceConfig sourceConfig, String pkgUrl) {
            lastMethod = "updateSourceWithUrl";
            lastConfig = sourceConfig;
            lastPackage = pkgUrl;
        }

        @Override
        public void deleteSource(String tenant, String namespace, String source) {
            calls.add("deleteSource:" + path(tenant, namespace, source));
        }

        @Override
        public void startSource(String tenant, String namespace, String source) {
            calls.add("startSource:" + path(tenant, namespace, source));
        }

        @Override
        public void startSource(String tenant, String namespace, String source, int instanceId) {
            calls.add("startSource:" + path(tenant, namespace, source) + "/" + instanceId);
        }

        @Override
        public void stopSource(String tenant, String namespace, String source) {
            calls.add("stopSource:" + path(tenant, namespace, source));
        }

        @Override
        public void stopSource(String tenant, String namespace, String source, int instanceId) {
            calls.add("stopSource:" + path(tenant, namespace, source) + "/" + instanceId);
        }

        @Override
        public void restartSource(String tenant, String namespace, String source) {
            calls.add("restartSource:" + path(tenant, namespace, source));
        }

        @Override
        public void restartSource(String tenant, String namespace, String source, int instanceId) {
            calls.add("restartSource:" + path(tenant, namespace, source) + "/" + instanceId);
        }

        @Override
        public org.apache.pulsar.common.policies.data.SourceStatus getSourceStatus(
                String tenant, String namespace, String source)
                throws PulsarAdminException {
            calls.add("getSourceStatus:" + path(tenant, namespace, source));
            return status;
        }

        @Override
        public org.apache.pulsar.common.policies.data.SourceStatus.SourceInstanceStatus
                        .SourceInstanceStatusData
                getSourceStatus(String tenant, String namespace, String source, int instanceId)
                        throws PulsarAdminException {
            calls.add("getSourceStatus:" + path(tenant, namespace, source) + "/" + instanceId);
            return instanceStatus;
        }

        private static String path(String tenant, String namespace, String source) {
            return tenant + "/" + namespace + "/" + source;
        }
    }
}
