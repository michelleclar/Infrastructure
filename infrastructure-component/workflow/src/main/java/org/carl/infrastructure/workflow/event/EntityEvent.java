package org.carl.infrastructure.workflow.event;

import org.carl.infrastructure.workflow.persistence.domain.snapshot.EntityCompensationDto;
import org.carl.infrastructure.workflow.persistence.domain.snapshot.EntityStateSnapshotDto;
import org.carl.infrastructure.workflow.persistence.domain.snapshot.EntityStateTransitionDto;

public class EntityEvent {
    private EventTypeEnum eventTypeEnum;
    private EntityStateSnapshotDto entityStateSnapshotDto;
    private EntityCompensationDto entityCompensationDto;
    private EntityStateTransitionDto entityStateTransitionDto;

    public EventTypeEnum getEventTypeEnum() {
        return eventTypeEnum;
    }

    public EntityEvent setEventTypeEnum(EventTypeEnum eventTypeEnum) {
        this.eventTypeEnum = eventTypeEnum;
        return this;
    }

    public EntityStateSnapshotDto getEntityStateSnapshotDto() {
        return entityStateSnapshotDto;
    }

    public EntityEvent setEntityStateSnapshotDto(EntityStateSnapshotDto entityStateSnapshotDto) {
        this.entityStateSnapshotDto = entityStateSnapshotDto;
        return this;
    }

    public EntityCompensationDto getEntityCompensationDto() {
        return entityCompensationDto;
    }

    public EntityEvent setEntityCompensationDto(EntityCompensationDto entityCompensationDto) {
        this.entityCompensationDto = entityCompensationDto;
        return this;
    }

    public EntityStateTransitionDto getEntityStateTransitionHistory() {
        return entityStateTransitionDto;
    }

    public EntityEvent setEntityStateTransitionHistoryDto(
            EntityStateTransitionDto entityStateTransitionHistory) {
        this.entityStateTransitionDto = entityStateTransitionHistory;
        return this;
    }
}
