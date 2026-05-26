package org.carl.infrastructure.audit.core;

import org.carl.infrastructure.audit.IAuditOperations;
import org.carl.infrastructure.audit.model.AuditEvent;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class AuditContext implements IAuditOperations {

    private static final ThreadLocal<String> CURRENT_OPERATOR =
            ThreadLocal.withInitial(() -> "system");

    private final ConcurrentLinkedDeque<AuditEvent> store = new ConcurrentLinkedDeque<>();
    private final AtomicLong idSequence = new AtomicLong(0);

    public static String getCurrentOperator() {
        return CURRENT_OPERATOR.get();
    }

    public static void setCurrentOperator(String operator) {
        CURRENT_OPERATOR.set(operator);
    }

    public static void clearCurrentOperator() {
        CURRENT_OPERATOR.remove();
    }

    @Override
    public void record(AuditEvent event) {
        store.addLast(event);
    }

    @Override
    public void record(String entityType, String entityId, String action, String details) {
        AuditEvent event = new AuditEvent(
                idSequence.incrementAndGet(),
                entityType,
                entityId,
                action,
                getCurrentOperator(),
                details,
                Instant.now()
        );
        record(event);
    }

    @Override
    public List<AuditEvent> query(String entityType, String entityId) {
        return store.stream()
                .filter(e -> entityType.equals(e.getEntityType())
                        && entityId.equals(e.getEntityId()))
                .collect(Collectors.toList());
    }
}
