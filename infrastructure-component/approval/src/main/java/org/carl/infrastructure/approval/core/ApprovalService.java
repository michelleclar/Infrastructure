package org.carl.infrastructure.approval.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.infrastructure.approval.model.ActionType;
import org.carl.infrastructure.approval.model.ApprovalDoneItem;
import org.carl.infrastructure.approval.model.ApprovalHistory;
import org.carl.infrastructure.approval.model.ApprovalInstance;
import org.carl.infrastructure.approval.model.ApprovalNode;
import org.carl.infrastructure.approval.model.ApprovalStatus;
import org.carl.infrastructure.approval.model.ApprovalTask;
import org.carl.infrastructure.approval.model.TaskStatus;
import org.carl.infrastructure.approval.user.UserContext;

import java.util.List;

@ApplicationScoped
public class ApprovalService {

    @Inject ApprovalRepository repository;
    @Inject ObjectMapper mapper;

    public long startProcess(String bizKey, List<ApprovalNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("nodes is required");
        }
        String nodesJson = writeNodesJson(nodes);
        ApprovalInstance instance = new ApprovalInstance();
        instance.setBizKey(bizKey);
        instance.setStatus(ApprovalStatus.IN_PROGRESS);
        instance.setCurrentStep(0);
        instance.setNodesJson(nodesJson);
        instance.setCreatedBy(UserContext.getCurrentUserId());

        long instanceId = repository.insertInstance(instance);
        ApprovalNode first = nodes.get(0);
        ApprovalTask task = new ApprovalTask();
        task.setInstanceId(instanceId);
        task.setNodeId(first.getNodeId());
        task.setAssignee(first.getAssignee());
        task.setStatus(TaskStatus.PENDING);
        long taskId = repository.insertTask(task);

        insertHistory(instanceId, taskId, ActionType.START, null);
        return instanceId;
    }

    public void approveTask(long taskId, String comment) {
        ApprovalTask task =
                repository
                        .fetchTask(taskId)
                        .orElseThrow(() -> new IllegalArgumentException("task not found"));
        String operator = requireOperator(task);
        if (task.getStatus() != TaskStatus.PENDING) {
            throw new IllegalStateException("task is not pending");
        }
        ApprovalInstance instance =
                repository
                        .fetchInstance(task.getInstanceId())
                        .orElseThrow(() -> new IllegalArgumentException("instance not found"));

        List<ApprovalNode> nodes = readNodes(instance.getNodesJson());
        int currentStep = instance.getCurrentStep();
        if (currentStep >= nodes.size()) {
            throw new IllegalStateException("invalid current step");
        }

        repository.updateTaskStatus(taskId, TaskStatus.DONE);
        insertHistory(instance.getId(), taskId, ActionType.APPROVE, comment);

        if (currentStep >= nodes.size() - 1) {
            repository.updateInstanceProgress(
                    instance.getId(), currentStep, ApprovalStatus.APPROVED.name());
            return;
        }

        ApprovalNode next = nodes.get(currentStep + 1);
        ApprovalTask nextTask = new ApprovalTask();
        nextTask.setInstanceId(instance.getId());
        nextTask.setNodeId(next.getNodeId());
        nextTask.setAssignee(next.getAssignee());
        nextTask.setStatus(TaskStatus.PENDING);
        repository.insertTask(nextTask);
        repository.updateInstanceProgress(
                instance.getId(), currentStep + 1, ApprovalStatus.IN_PROGRESS.name());
    }

    public void backTask(long taskId, String comment) {
        ApprovalTask task =
                repository
                        .fetchTask(taskId)
                        .orElseThrow(() -> new IllegalArgumentException("task not found"));
        requireOperator(task);
        if (task.getStatus() != TaskStatus.PENDING) {
            throw new IllegalStateException("task is not pending");
        }
        ApprovalInstance instance =
                repository
                        .fetchInstance(task.getInstanceId())
                        .orElseThrow(() -> new IllegalArgumentException("instance not found"));
        List<ApprovalNode> nodes = readNodes(instance.getNodesJson());
        int currentStep = instance.getCurrentStep();
        if (currentStep <= 0) {
            throw new IllegalStateException("cannot back from first step");
        }

        repository.updateTaskStatus(taskId, TaskStatus.BACKED);
        insertHistory(instance.getId(), taskId, ActionType.BACK, comment);

        ApprovalNode prev = nodes.get(currentStep - 1);
        ApprovalTask backTask = new ApprovalTask();
        backTask.setInstanceId(instance.getId());
        backTask.setNodeId(prev.getNodeId());
        backTask.setAssignee(prev.getAssignee());
        backTask.setStatus(TaskStatus.PENDING);
        repository.insertTask(backTask);
        repository.updateInstanceProgress(
                instance.getId(), currentStep - 1, ApprovalStatus.IN_PROGRESS.name());
    }

    public void transferTask(long taskId, String toUserId, String comment) {
        ApprovalTask task =
                repository
                        .fetchTask(taskId)
                        .orElseThrow(() -> new IllegalArgumentException("task not found"));
        requireOperator(task);
        if (task.getStatus() != TaskStatus.PENDING) {
            throw new IllegalStateException("task is not pending");
        }
        repository.updateTaskAssignee(taskId, toUserId);
        insertHistory(task.getInstanceId(), taskId, ActionType.TRANSFER, comment);
    }

    public List<ApprovalTask> listTodo() {
        String operator = UserContext.getCurrentUserId();
        return repository.fetchTodoTasks(operator);
    }

    public List<ApprovalDoneItem> listDone() {
        String operator = UserContext.getCurrentUserId();
        return repository.fetchDoneItems(operator);
    }

    private String requireOperator(ApprovalTask task) {
        String operator = UserContext.getCurrentUserId();
        if (operator == null || operator.isBlank()) {
            throw new IllegalStateException("operator is required");
        }
        if (!operator.equals(task.getAssignee())) {
            throw new IllegalStateException("task does not belong to operator");
        }
        return operator;
    }

    private void insertHistory(long instanceId, long taskId, ActionType action, String comment) {
        ApprovalHistory history = new ApprovalHistory();
        history.setInstanceId(instanceId);
        history.setTaskId(taskId);
        history.setAction(action);
        history.setOperator(UserContext.getCurrentUserId());
        history.setComment(comment);
        repository.insertHistory(history);
    }

    private String writeNodesJson(List<ApprovalNode> nodes) {
        try {
            return mapper.writeValueAsString(nodes);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize nodes failed", e);
        }
    }

    private List<ApprovalNode> readNodes(String nodesJson) {
        try {
            return mapper.readValue(
                    nodesJson,
                    mapper.getTypeFactory()
                            .constructCollectionType(List.class, ApprovalNode.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("parse nodes failed", e);
        }
    }
}
