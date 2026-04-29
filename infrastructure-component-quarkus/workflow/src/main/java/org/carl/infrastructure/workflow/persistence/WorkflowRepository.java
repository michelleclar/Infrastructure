package org.carl.infrastructure.workflow.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.infrastructure.workflow.gen.Tables;
import org.carl.infrastructure.workflow.gen.tables.pojos.EntityStateTransitionHistoryDo;
import org.carl.infrastructure.workflow.gen.tables.records.EntityStateSnapshotRecord;
import org.carl.infrastructure.workflow.gen.tables.records.EntityStateTransitionHistoryRecord;
import org.carl.infrastructure.workflow.persistence.domain.snapshot.EntityCompensationDto;
import org.carl.infrastructure.workflow.persistence.domain.snapshot.EntityStateSnapshotDto;
import org.carl.infrastructure.workflow.persistence.domain.snapshot.EntityStateTransitionDto;
import org.jooq.DSLContext;

import java.time.LocalDateTime;

/**
 * Workflow State Storage Repository Manages persistence operations for state entities, state
 * transition history, and compensation logs
 */
@ApplicationScoped
// @IfBuildProperty(name = "workflow.enable.log", stringValue = "true")
public class WorkflowRepository {

    @Inject DSLContext dsl;

    /**
     * 处理工作流执行时的事件
     *
     * <p>添加快照表,记录状态转换表(state:pending)
     *
     * @param dto
     */
    public void upsertEntityStateSnapshot(EntityStateSnapshotDto dto) {
        dsl.transaction(
                transaction -> {
                    DSLContext txDsl = transaction.dsl();
                    EntityStateSnapshotRecord entityStateSnapshotRecord =
                            txDsl.selectFrom(Tables.ENTITY_STATE_SNAPSHOT)
                                    .where(
                                            Tables.ENTITY_STATE_SNAPSHOT.WORKFLOW_STAMP.eq(
                                                    dto.getWorkflowStamp()))
                                    .fetchOne();
                    EntityStateTransitionDto entityStateTransitionDto =
                            dto.toEntityStateTransitionDto();

                    if (entityStateSnapshotRecord != null) {
                        // NOTE: update workflow id ,  state_data , workflow run id,
                        // state_version,last_transition_id
                        entityStateTransitionDto.setTransitionParentId(
                                entityStateSnapshotRecord.getLastTransitionId());
                        EntityStateTransitionHistoryRecord entityStateTransitionHistoryRecord =
                                saveEntityStateTransfer(entityStateTransitionDto);
                        dsl.update(Tables.ENTITY_STATE_SNAPSHOT)
                                .set(
                                        Tables.ENTITY_STATE_SNAPSHOT.CURRENT_STATE,
                                        entityStateSnapshotRecord.getCurrentState())
                                .set(
                                        Tables.ENTITY_STATE_SNAPSHOT.WORKFLOW_ID,
                                        entityStateSnapshotRecord.getWorkflowId())
                                .set(
                                        Tables.ENTITY_STATE_SNAPSHOT.WORKFLOW_RUN_ID,
                                        entityStateSnapshotRecord.getWorkflowRunId())
                                .set(
                                        Tables.ENTITY_STATE_SNAPSHOT.LAST_TRANSITION_ID,
                                        entityStateTransitionHistoryRecord.getTransitionId())
                                .set(
                                        Tables.ENTITY_STATE_SNAPSHOT.STATE_VERSION,
                                        entityStateSnapshotRecord.getStateVersion() + 1)
                                .set(Tables.ENTITY_STATE_SNAPSHOT.UPDATED_AT, LocalDateTime.now())
                                .execute();
                    } else {
                        // NOTE: first record is not transition_id
                        EntityStateTransitionHistoryRecord entityStateTransitionHistoryRecord =
                                saveEntityStateTransfer(entityStateTransitionDto);
                        dsl.insertInto(Tables.ENTITY_STATE_SNAPSHOT)
                                .set(Tables.ENTITY_STATE_SNAPSHOT.ENTITY_ID, dto.getEntityId())
                                .set(Tables.ENTITY_STATE_SNAPSHOT.ENTITY_TYPE, dto.getEntityType())
                                .set(
                                        Tables.ENTITY_STATE_SNAPSHOT.WORKFLOW_STAMP,
                                        dto.getWorkflowStamp())
                                .set(
                                        Tables.ENTITY_STATE_SNAPSHOT.CURRENT_STATE,
                                        dto.getCurrentState())
                                .set(Tables.ENTITY_STATE_SNAPSHOT.WORKFLOW_ID, dto.getWorkflowId())
                                .set(
                                        Tables.ENTITY_STATE_SNAPSHOT.WORKFLOW_RUN_ID,
                                        dto.getWorkflowRunId())
                                .set(Tables.ENTITY_STATE_SNAPSHOT.STATE_VERSION, 1L) // 初始版本为1
                                .set(
                                        Tables.ENTITY_STATE_SNAPSHOT.LAST_TRANSITION_ID,
                                        entityStateTransitionHistoryRecord.getTransitionId())
                                .set(Tables.ENTITY_STATE_SNAPSHOT.STATE_DATA, dto.getStateData())
                                .execute();
                    }
                });
    }

    public EntityStateTransitionHistoryRecord saveEntityStateTransfer(
            EntityStateTransitionDto dto) {
        EntityStateTransitionHistoryDo doRecord = new EntityStateTransitionHistoryDo();
        return null;
    }

    public void upsertEntityTransfer(EntityStateTransitionDto dto) {
        dsl.transaction(
                transaction -> {
                    DSLContext txDsl = transaction.dsl();
                    // query snapshot
                    EntityStateSnapshotRecord entityStateSnapshotRecord =
                            txDsl.selectFrom(Tables.ENTITY_STATE_SNAPSHOT)
                                    .where(
                                            Tables.ENTITY_STATE_SNAPSHOT.WORKFLOW_STAMP.eq(
                                                    dto.getWorkflowStamp()))
                                    .fetchOne();
                    if (entityStateSnapshotRecord != null) {

                        dsl.insertInto(Tables.ENTITY_STATE_TRANSITION_HISTORY)
                                .set(
                                        Tables.ENTITY_STATE_TRANSITION_HISTORY.ENTITY_ID,
                                        entityStateSnapshotRecord.getEntityId())
                                .set(
                                        Tables.ENTITY_STATE_TRANSITION_HISTORY.ENTITY_TYPE,
                                        dto.getEntityType())
                                .set(
                                        Tables.ENTITY_STATE_TRANSITION_HISTORY.WORKFLOW_ID,
                                        dto.getWorkflowId())
                                .set(
                                        Tables.ENTITY_STATE_TRANSITION_HISTORY.WORKFLOW_RUN_ID,
                                        dto.getWorkflowRunId());

                        // update last_transition_id
                        dsl.update(Tables.ENTITY_STATE_SNAPSHOT)
                                .set(
                                        Tables.ENTITY_STATE_SNAPSHOT.LAST_TRANSITION_ID,
                                        entityStateSnapshotRecord.getLastTransitionId());
                    }
                });
    }

    /**
     * 补偿操作
     *
     * @param compensationDto {@link EntityCompensationDto}
     */
    public void updateCompensationStatus(EntityCompensationDto compensationDto) {}
}
