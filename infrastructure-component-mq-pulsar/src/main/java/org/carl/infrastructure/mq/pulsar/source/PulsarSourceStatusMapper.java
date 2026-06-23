package org.carl.infrastructure.mq.pulsar.source;

import org.apache.pulsar.common.policies.data.ExceptionInformation;
import org.carl.infrastructure.mq.source.SourceExceptionInfo;
import org.carl.infrastructure.mq.source.SourceInstanceStatus;
import org.carl.infrastructure.mq.source.SourceStatus;

import java.util.List;

final class PulsarSourceStatusMapper {

    private PulsarSourceStatusMapper() {}

    static SourceStatus toApi(org.apache.pulsar.common.policies.data.SourceStatus status) {
        if (status == null) {
            return new SourceStatus(0, 0, List.of());
        }
        return new SourceStatus(
                status.getNumInstances(),
                status.getNumRunning(),
                status.getInstances() == null
                        ? List.of()
                        : status.getInstances().stream()
                                .map(
                                        instance ->
                                                toApi(instance.getInstanceId(), instance.getStatus()))
                                .toList());
    }

    static SourceInstanceStatus toApi(
            int instanceId,
            org.apache.pulsar.common.policies.data.SourceStatus.SourceInstanceStatus
                            .SourceInstanceStatusData
                    status) {
        if (status == null) {
            return new SourceInstanceStatus(
                    instanceId, false, null, 0, 0, 0, List.of(), 0, List.of(), 0, 0, null);
        }
        return new SourceInstanceStatus(
                instanceId,
                status.isRunning(),
                status.getError(),
                status.getNumRestarts(),
                status.getNumReceivedFromSource(),
                status.getNumSystemExceptions(),
                exceptions(status.getLatestSystemExceptions()),
                status.getNumSourceExceptions(),
                exceptions(status.getLatestSourceExceptions()),
                status.getNumWritten(),
                status.getLastReceivedTime(),
                status.getWorkerId());
    }

    private static List<SourceExceptionInfo> exceptions(List<ExceptionInformation> exceptions) {
        if (exceptions == null || exceptions.isEmpty()) {
            return List.of();
        }
        return exceptions.stream()
                .map(
                        exception ->
                                new SourceExceptionInfo(
                                        exception.getExceptionString(), exception.getTimestampMs()))
                .toList();
    }
}
