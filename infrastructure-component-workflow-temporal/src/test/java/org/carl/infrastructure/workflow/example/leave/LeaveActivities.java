package org.carl.infrastructure.workflow.example.leave;

import org.carl.infrastructure.workflow.runtime.BusinessActivityRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stub business activities for the leave workflow end-to-end tests.
 *
 * <p>Each activity is a simple {@code Map<String,Object> -> Map<String,Object>} function, mirroring
 * the contract enforced by {@link BusinessActivityRegistry}.
 */
final class LeaveActivities {

    private LeaveActivities() {
        throw new AssertionError("no instances");
    }

    static BusinessActivityRegistry buildRegistry() {
        return buildRegistry(new AtomicInteger(), new AtomicInteger());
    }

    /**
     * Build a registry that increments the supplied counters on each invocation of the named
     * activities. Used by the back-edge integration test to assert that {@code requestLeave} is
     * actually re-executed after a rejection.
     */
    static BusinessActivityRegistry buildRegistry(
            AtomicInteger createLeaveRequestInvocations, AtomicInteger notifyManagerInvocations) {
        BusinessActivityRegistry registry = new BusinessActivityRegistry();
        registry.register(
                "createLeaveRequest",
                input -> {
                    createLeaveRequestInvocations.incrementAndGet();
                    Map<String, Object> out = new LinkedHashMap<>();
                    Object employeeId = input == null ? null : input.get("employeeId");
                    out.put(
                            "requestId",
                            "REQ-"
                                    + UUID.nameUUIDFromBytes(String.valueOf(employeeId).getBytes())
                                            .toString()
                                            .substring(0, 8));
                    out.put("employeeId", employeeId);
                    return out;
                });
        registry.register(
                "notifyManager",
                input -> {
                    notifyManagerInvocations.incrementAndGet();
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("notified", true);
                    return out;
                });
        return registry;
    }
}
