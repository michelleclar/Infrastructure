package org.carl.infrastructure.workflow.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Guards against drift between {@link NodeTypes} (string constants) and {@link BuiltInNodeType}
 * (enum) — they must always carry identical wire-format values.
 */
class BuiltInNodeTypeAlignmentTest {

    @Test
    void serviceTask_alignsAcrossSources() {
        assertEquals(NodeTypes.SERVICE_TASK, BuiltInNodeType.SERVICE_TASK.value());
    }

    @Test
    void approvalTask_alignsAcrossSources() {
        assertEquals(NodeTypes.APPROVAL_TASK, BuiltInNodeType.APPROVAL_TASK.value());
    }

    @Test
    void userTask_alignsAcrossSources() {
        assertEquals(NodeTypes.USER_TASK, BuiltInNodeType.USER_TASK.value());
    }

    @Test
    void eventTask_alignsAcrossSources() {
        assertEquals(NodeTypes.EVENT_TASK, BuiltInNodeType.EVENT_TASK.value());
    }

    @Test
    void timerTask_alignsAcrossSources() {
        assertEquals(NodeTypes.TIMER_TASK, BuiltInNodeType.TIMER_TASK.value());
    }

    @Test
    void taskGroup_alignsAcrossSources() {
        assertEquals(NodeTypes.TASK_GROUP, BuiltInNodeType.TASK_GROUP.value());
    }

    @Test
    void gateway_alignsAcrossSources() {
        assertEquals(NodeTypes.GATEWAY, BuiltInNodeType.GATEWAY.value());
    }

    @Test
    void subProcess_alignsAcrossSources() {
        assertEquals(NodeTypes.SUB_PROCESS, BuiltInNodeType.SUB_PROCESS.value());
    }

    @Test
    void endTask_alignsAcrossSources() {
        assertEquals(NodeTypes.END_TASK, BuiltInNodeType.END_TASK.value());
    }

    @Test
    void enumSize_matchesNineBuiltIns() {
        assertEquals(9, BuiltInNodeType.values().length);
    }
}
