package org.carl.infrastructure.workflow.event;

import org.carl.infrastructure.workflow.persistence.domain.snapshot.EntityCompensationDto;
import org.carl.infrastructure.workflow.persistence.domain.snapshot.EntityStateSnapshotDto;
import org.carl.infrastructure.workflow.persistence.domain.snapshot.EntityStateTransitionDto;

public class EntityEventFactory {
    public static EntityEvent create(EntityStateSnapshotDto entityStateSnapshotDto) {
        EntityEvent entityEvent = new EntityEvent();
        entityEvent.setEntityStateSnapshotDto(entityStateSnapshotDto);
        entityEvent.setEventTypeEnum(EventTypeEnum.snapshot);
        return entityEvent;
    }

    public static EntityEvent create(EntityCompensationDto dto) {
        EntityEvent entityEvent = new EntityEvent();
        entityEvent.setEntityCompensationDto(dto);
        entityEvent.setEventTypeEnum(EventTypeEnum.compensation);
        return entityEvent;
    }

    public static EntityEvent create(EntityStateTransitionDto dto) {
        EntityEvent entityEvent = new EntityEvent();
        entityEvent.setEntityStateTransitionHistoryDto(dto);
        entityEvent.setEventTypeEnum(EventTypeEnum.transfer);
        return entityEvent;
    }
}
