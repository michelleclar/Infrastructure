package org.carl.infrastructure.approval.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.carl.infrastructure.approval.model.ApprovalInstance;
import org.carl.infrastructure.approval.model.ApprovalNode;
import org.carl.infrastructure.approval.model.ApprovalStatus;
import org.carl.infrastructure.approval.model.ApprovalTask;
import org.carl.infrastructure.approval.model.TaskStatus;
import org.carl.infrastructure.approval.user.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock ApprovalRepository repository;
    @InjectMocks ApprovalService service;

    @BeforeEach
    void setUp() throws Exception {
        Field mapperField = ApprovalService.class.getDeclaredField("mapper");
        mapperField.setAccessible(true);
        mapperField.set(service, new ObjectMapper());
        UserContext.setCurrentUserId("user1");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void startProcess_throwsWhenNodesEmpty() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.startProcess("biz-1", List.of()));
    }

    @Test
    void startProcess_throwsWhenNodesNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.startProcess("biz-1", null));
    }

    @Test
    void approveTask_throwsWhenTaskNotFound() {
        when(repository.fetchTask(1L)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.approveTask(1L, "ok"));
    }

    @Test
    void approveTask_throwsWhenTaskNotPending() {
        ApprovalTask task = new ApprovalTask();
        task.setId(1L);
        task.setInstanceId(10L);
        task.setAssignee("user1");
        task.setStatus(TaskStatus.DONE);

        when(repository.fetchTask(1L)).thenReturn(Optional.of(task));

        assertThrows(
                IllegalStateException.class,
                () -> service.approveTask(1L, "ok"));
    }

    @Test
    void approveTask_advancesToNextNodeWhenNotLastStep() throws Exception {
        ApprovalNode node1 = new ApprovalNode();
        node1.setNodeId("n1");
        node1.setAssignee("user1");

        ApprovalNode node2 = new ApprovalNode();
        node2.setNodeId("n2");
        node2.setAssignee("user2");

        String nodesJson = new ObjectMapper().writeValueAsString(List.of(node1, node2));

        ApprovalTask task = new ApprovalTask();
        task.setId(1L);
        task.setInstanceId(10L);
        task.setAssignee("user1");
        task.setStatus(TaskStatus.PENDING);

        ApprovalInstance instance = new ApprovalInstance();
        instance.setId(10L);
        instance.setCurrentStep(0);
        instance.setStatus(ApprovalStatus.IN_PROGRESS);
        instance.setNodesJson(nodesJson);

        when(repository.fetchTask(1L)).thenReturn(Optional.of(task));
        when(repository.fetchInstance(10L)).thenReturn(Optional.of(instance));
        when(repository.insertTask(any())).thenReturn(2L);

        service.approveTask(1L, "lgtm");

        verify(repository).updateTaskStatus(1L, TaskStatus.DONE);
        verify(repository).insertTask(any());
        verify(repository).updateInstanceProgress(10L, 1, ApprovalStatus.IN_PROGRESS.name());
    }

    @Test
    void approveTask_marksInstanceApprovedOnLastStep() throws Exception {
        ApprovalNode node1 = new ApprovalNode();
        node1.setNodeId("n1");
        node1.setAssignee("user1");

        String nodesJson = new ObjectMapper().writeValueAsString(List.of(node1));

        ApprovalTask task = new ApprovalTask();
        task.setId(1L);
        task.setInstanceId(10L);
        task.setAssignee("user1");
        task.setStatus(TaskStatus.PENDING);

        ApprovalInstance instance = new ApprovalInstance();
        instance.setId(10L);
        instance.setCurrentStep(0);
        instance.setStatus(ApprovalStatus.IN_PROGRESS);
        instance.setNodesJson(nodesJson);

        when(repository.fetchTask(1L)).thenReturn(Optional.of(task));
        when(repository.fetchInstance(10L)).thenReturn(Optional.of(instance));

        service.approveTask(1L, "approved");

        verify(repository).updateTaskStatus(1L, TaskStatus.DONE);
        verify(repository, never()).insertTask(any());
        verify(repository).updateInstanceProgress(10L, 0, ApprovalStatus.APPROVED.name());
    }

    @Test
    void backTask_throwsWhenTaskNotPending() {
        ApprovalTask task = new ApprovalTask();
        task.setId(1L);
        task.setInstanceId(10L);
        task.setAssignee("user1");
        task.setStatus(TaskStatus.DONE);

        when(repository.fetchTask(1L)).thenReturn(Optional.of(task));

        assertThrows(
                IllegalStateException.class,
                () -> service.backTask(1L, "back"));
    }

    @Test
    void backTask_throwsWhenAtFirstStep() throws Exception {
        ApprovalNode node1 = new ApprovalNode();
        node1.setNodeId("n1");
        node1.setAssignee("user1");

        String nodesJson = new ObjectMapper().writeValueAsString(List.of(node1));

        ApprovalTask task = new ApprovalTask();
        task.setId(1L);
        task.setInstanceId(10L);
        task.setAssignee("user1");
        task.setStatus(TaskStatus.PENDING);

        ApprovalInstance instance = new ApprovalInstance();
        instance.setId(10L);
        instance.setCurrentStep(0);
        instance.setStatus(ApprovalStatus.IN_PROGRESS);
        instance.setNodesJson(nodesJson);

        when(repository.fetchTask(1L)).thenReturn(Optional.of(task));
        when(repository.fetchInstance(10L)).thenReturn(Optional.of(instance));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.backTask(1L, "back"));
        assertEquals("cannot back from first step", ex.getMessage());
    }

    @Test
    void backTask_createsNewTaskAtPreviousNode() throws Exception {
        ApprovalNode node1 = new ApprovalNode();
        node1.setNodeId("n1");
        node1.setAssignee("user0");

        ApprovalNode node2 = new ApprovalNode();
        node2.setNodeId("n2");
        node2.setAssignee("user1");

        String nodesJson = new ObjectMapper().writeValueAsString(List.of(node1, node2));

        ApprovalTask task = new ApprovalTask();
        task.setId(2L);
        task.setInstanceId(10L);
        task.setAssignee("user1");
        task.setStatus(TaskStatus.PENDING);

        ApprovalInstance instance = new ApprovalInstance();
        instance.setId(10L);
        instance.setCurrentStep(1);
        instance.setStatus(ApprovalStatus.IN_PROGRESS);
        instance.setNodesJson(nodesJson);

        when(repository.fetchTask(2L)).thenReturn(Optional.of(task));
        when(repository.fetchInstance(10L)).thenReturn(Optional.of(instance));
        when(repository.insertTask(any())).thenReturn(3L);

        service.backTask(2L, "needs rework");

        verify(repository).updateTaskStatus(2L, TaskStatus.BACKED);
        verify(repository).insertTask(argThat(t -> "user0".equals(t.getAssignee())));
        verify(repository).updateInstanceProgress(10L, 0, ApprovalStatus.IN_PROGRESS.name());
    }

    @Test
    void transferTask_throwsWhenTaskNotPending() {
        ApprovalTask task = new ApprovalTask();
        task.setId(1L);
        task.setInstanceId(10L);
        task.setAssignee("user1");
        task.setStatus(TaskStatus.DONE);

        when(repository.fetchTask(1L)).thenReturn(Optional.of(task));

        assertThrows(
                IllegalStateException.class,
                () -> service.transferTask(1L, "user2", "transfer"));
    }

    @Test
    void transferTask_updatesAssigneeAndInsertsHistory() {
        ApprovalTask task = new ApprovalTask();
        task.setId(1L);
        task.setInstanceId(10L);
        task.setAssignee("user1");
        task.setStatus(TaskStatus.PENDING);

        when(repository.fetchTask(1L)).thenReturn(Optional.of(task));

        service.transferTask(1L, "user2", "transfer");

        verify(repository).updateTaskAssignee(1L, "user2");
        verify(repository).insertHistory(any());
    }
}
