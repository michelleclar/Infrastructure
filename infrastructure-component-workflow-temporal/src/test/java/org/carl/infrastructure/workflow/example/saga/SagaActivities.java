package org.carl.infrastructure.workflow.example.saga;

import org.carl.infrastructure.workflow.runtime.BusinessActivityRegistry;

import java.util.List;
import java.util.Map;

/** Activity registry builder for saga tests. Shares a {@code callLog} for assertion. */
final class SagaActivities {

    private SagaActivities() {
        throw new AssertionError("no instances");
    }

    /**
     * Build a registry whose activities append their name to {@code callLog}. {@code
     * sendNotification} always throws so the workflow fails and triggers compensation.
     */
    static BusinessActivityRegistry buildRegistry(List<String> callLog) {
        BusinessActivityRegistry reg = new BusinessActivityRegistry();

        reg.register(
                "createOrder",
                input -> {
                    callLog.add("createOrder");
                    return Map.of("orderId", "ORD-001");
                });
        reg.register(
                "reserveBudget",
                input -> {
                    callLog.add("reserveBudget");
                    return Map.of();
                });
        reg.register(
                "sendNotification",
                input -> {
                    callLog.add("sendNotification");
                    throw new RuntimeException("notification service unavailable");
                });
        reg.register(
                "cancelOrder",
                input -> {
                    callLog.add("cancelOrder");
                    return Map.of();
                });
        reg.register(
                "releaseBudget",
                input -> {
                    callLog.add("releaseBudget");
                    return Map.of();
                });

        return reg;
    }
}
