package org.carl.infrastructure.workflow.build;

public class GeneraterWorkflowStampId {
    public static String create(String entityId, String entityType) {
        return create(entityId, entityType, "_");
    }

    public static String create(String entityId, String entityType, String join) {
        return entityType + join + entityId;
    }
}
