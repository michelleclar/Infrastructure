package org.carl.infrastructure.approval.core;

import static org.carl.infrastructure.approval.core.ApprovalTables.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.infrastructure.approval.model.ActionType;
import org.carl.infrastructure.approval.model.ApprovalDoneItem;
import org.carl.infrastructure.approval.model.ApprovalHistory;
import org.carl.infrastructure.approval.model.ApprovalInstance;
import org.carl.infrastructure.approval.model.ApprovalTask;
import org.carl.infrastructure.approval.model.TaskStatus;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ApprovalRepository {

    @Inject DSLContext dsl;

    public long insertInstance(ApprovalInstance instance) {
        LocalDateTime now = LocalDateTime.now();
        Long id =
                dsl.insertInto(APPROVAL_INSTANCE)
                        .set(INSTANCE_BIZ_KEY, instance.getBizKey())
                        .set(INSTANCE_STATUS, instance.getStatus().name())
                        .set(INSTANCE_CURRENT_STEP, instance.getCurrentStep())
                        .set(INSTANCE_NODES_JSON, instance.getNodesJson())
                        .set(INSTANCE_CREATED_BY, instance.getCreatedBy())
                        .set(INSTANCE_CREATED_AT, now)
                        .set(INSTANCE_UPDATED_AT, now)
                        .returning(INSTANCE_ID)
                        .fetchOne()
                        .get(INSTANCE_ID);
        instance.setId(id);
        instance.setCreatedAt(now);
        instance.setUpdatedAt(now);
        return id;
    }

    public Optional<ApprovalInstance> fetchInstance(long instanceId) {
        Record record =
                dsl.select(
                                INSTANCE_ID,
                                INSTANCE_BIZ_KEY,
                                INSTANCE_STATUS,
                                INSTANCE_CURRENT_STEP,
                                INSTANCE_NODES_JSON,
                                INSTANCE_CREATED_BY,
                                INSTANCE_CREATED_AT,
                                INSTANCE_UPDATED_AT)
                        .from(APPROVAL_INSTANCE)
                        .where(INSTANCE_ID.eq(instanceId))
                        .fetchOne();
        if (record == null) {
            return Optional.empty();
        }
        ApprovalInstance instance = new ApprovalInstance();
        instance.setId(record.get(INSTANCE_ID));
        instance.setBizKey(record.get(INSTANCE_BIZ_KEY));
        instance.setStatus(
                Enum.valueOf(
                        org.carl.infrastructure.approval.model.ApprovalStatus.class,
                        record.get(INSTANCE_STATUS)));
        instance.setCurrentStep(record.get(INSTANCE_CURRENT_STEP));
        instance.setNodesJson(record.get(INSTANCE_NODES_JSON));
        instance.setCreatedBy(record.get(INSTANCE_CREATED_BY));
        instance.setCreatedAt(record.get(INSTANCE_CREATED_AT));
        instance.setUpdatedAt(record.get(INSTANCE_UPDATED_AT));
        return Optional.of(instance);
    }

    public void updateInstanceProgress(long instanceId, int currentStep, String status) {
        dsl.update(APPROVAL_INSTANCE)
                .set(INSTANCE_CURRENT_STEP, currentStep)
                .set(INSTANCE_STATUS, status)
                .set(INSTANCE_UPDATED_AT, LocalDateTime.now())
                .where(INSTANCE_ID.eq(instanceId))
                .execute();
    }

    public long insertTask(ApprovalTask task) {
        LocalDateTime now = LocalDateTime.now();
        Long id =
                dsl.insertInto(APPROVAL_TASK)
                        .set(TASK_INSTANCE_ID, task.getInstanceId())
                        .set(TASK_NODE_ID, task.getNodeId())
                        .set(TASK_ASSIGNEE, task.getAssignee())
                        .set(TASK_STATUS, task.getStatus().name())
                        .set(TASK_CREATED_AT, now)
                        .set(TASK_UPDATED_AT, now)
                        .returning(TASK_ID)
                        .fetchOne()
                        .get(TASK_ID);
        task.setId(id);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return id;
    }

    public Optional<ApprovalTask> fetchTask(long taskId) {
        Record record =
                dsl.select(
                                TASK_ID,
                                TASK_INSTANCE_ID,
                                TASK_NODE_ID,
                                TASK_ASSIGNEE,
                                TASK_STATUS,
                                TASK_CREATED_AT,
                                TASK_UPDATED_AT)
                        .from(APPROVAL_TASK)
                        .where(TASK_ID.eq(taskId))
                        .fetchOne();
        if (record == null) {
            return Optional.empty();
        }
        ApprovalTask task = new ApprovalTask();
        task.setId(record.get(TASK_ID));
        task.setInstanceId(record.get(TASK_INSTANCE_ID));
        task.setNodeId(record.get(TASK_NODE_ID));
        task.setAssignee(record.get(TASK_ASSIGNEE));
        task.setStatus(Enum.valueOf(TaskStatus.class, record.get(TASK_STATUS)));
        task.setCreatedAt(record.get(TASK_CREATED_AT));
        task.setUpdatedAt(record.get(TASK_UPDATED_AT));
        return Optional.of(task);
    }

    public void updateTaskStatus(long taskId, TaskStatus status) {
        dsl.update(APPROVAL_TASK)
                .set(TASK_STATUS, status.name())
                .set(TASK_UPDATED_AT, LocalDateTime.now())
                .where(TASK_ID.eq(taskId))
                .execute();
    }

    public void updateTaskAssignee(long taskId, String assignee) {
        dsl.update(APPROVAL_TASK)
                .set(TASK_ASSIGNEE, assignee)
                .set(TASK_UPDATED_AT, LocalDateTime.now())
                .where(TASK_ID.eq(taskId))
                .execute();
    }

    public List<ApprovalTask> fetchTodoTasks(String assignee) {
        return dsl.select(
                        TASK_ID,
                        TASK_INSTANCE_ID,
                        TASK_NODE_ID,
                        TASK_ASSIGNEE,
                        TASK_STATUS,
                        TASK_CREATED_AT,
                        TASK_UPDATED_AT)
                .from(APPROVAL_TASK)
                .where(TASK_ASSIGNEE.eq(assignee).and(TASK_STATUS.eq(TaskStatus.PENDING.name())))
                .orderBy(TASK_CREATED_AT.desc())
                .fetch(
                        record -> {
                            ApprovalTask task = new ApprovalTask();
                            task.setId(record.get(TASK_ID));
                            task.setInstanceId(record.get(TASK_INSTANCE_ID));
                            task.setNodeId(record.get(TASK_NODE_ID));
                            task.setAssignee(record.get(TASK_ASSIGNEE));
                            task.setStatus(Enum.valueOf(TaskStatus.class, record.get(TASK_STATUS)));
                            task.setCreatedAt(record.get(TASK_CREATED_AT));
                            task.setUpdatedAt(record.get(TASK_UPDATED_AT));
                            return task;
                        });
    }

    public List<ApprovalDoneItem> fetchDoneItems(String operator) {
        return dsl.select(
                        HISTORY_ID,
                        HISTORY_INSTANCE_ID,
                        HISTORY_TASK_ID,
                        HISTORY_ACTION,
                        HISTORY_OPERATOR,
                        HISTORY_COMMENT,
                        HISTORY_CREATED_AT,
                        TASK_NODE_ID)
                .from(APPROVAL_HISTORY)
                .leftJoin(APPROVAL_TASK)
                .on(HISTORY_TASK_ID.eq(TASK_ID))
                .where(HISTORY_OPERATOR.eq(operator))
                .orderBy(HISTORY_CREATED_AT.desc())
                .fetch(
                        record -> {
                            ApprovalDoneItem item = new ApprovalDoneItem();
                            item.setHistoryId(record.get(HISTORY_ID));
                            item.setInstanceId(record.get(HISTORY_INSTANCE_ID));
                            item.setTaskId(record.get(HISTORY_TASK_ID));
                            item.setNodeId(record.get(TASK_NODE_ID));
                            item.setAction(
                                    Enum.valueOf(ActionType.class, record.get(HISTORY_ACTION)));
                            item.setOperator(record.get(HISTORY_OPERATOR));
                            item.setComment(record.get(HISTORY_COMMENT));
                            item.setCreatedAt(record.get(HISTORY_CREATED_AT));
                            return item;
                        });
    }

    public void insertHistory(ApprovalHistory history) {
        LocalDateTime now = LocalDateTime.now();
        dsl.insertInto(APPROVAL_HISTORY)
                .set(HISTORY_INSTANCE_ID, history.getInstanceId())
                .set(HISTORY_TASK_ID, history.getTaskId())
                .set(HISTORY_ACTION, history.getAction().name())
                .set(HISTORY_OPERATOR, history.getOperator())
                .set(HISTORY_COMMENT, history.getComment())
                .set(HISTORY_CREATED_AT, now)
                .execute();
        history.setCreatedAt(now);
    }
}
