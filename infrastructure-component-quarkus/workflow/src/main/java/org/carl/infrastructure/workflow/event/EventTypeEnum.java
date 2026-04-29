package org.carl.infrastructure.workflow.event;

public enum EventTypeEnum {
    compensation,
    /** 工作流执行时事件类型 */
    snapshot,
    /** 工作流结束时世界类型 */
    transfer,
}
