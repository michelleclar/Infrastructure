package org.carl.infrastructure.workflow.test.publish;

import io.quarkus.test.junit.QuarkusTest;

import org.carl.infrastructure.statemachine.StateMachine;
import org.carl.infrastructure.statemachine.builder.StateMachineBuilder;
import org.carl.infrastructure.statemachine.builder.StateMachineBuilderFactory;
import org.carl.infrastructure.workflow.build.TXWorkflowBuilder;
import org.carl.infrastructure.workflow.test.model.ContentPublishingContext;
import org.carl.infrastructure.workflow.test.model.ContentPublishingEventEnum;
import org.carl.infrastructure.workflow.test.model.ContentPublishingStatusEnum;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ContentPublishingStateMachineImplStateMachineTest {
    @Test
    public void test() {
        ContentPublishingStateMachine contentPublishingStateMachine =
                new ContentPublishingStateMachine();

        StateMachineBuilder<
                        ContentPublishingStatusEnum,
                        ContentPublishingEventEnum,
                        ContentPublishingContext>
                builder = StateMachineBuilderFactory.create();
        contentPublishingStateMachine.externalTransition(builder);
        StateMachine<
                        ContentPublishingStatusEnum,
                        ContentPublishingEventEnum,
                        ContentPublishingContext>
                build = builder.build("content.publish");
        String entityId = "123456";
        ContentPublishingContext context = new ContentPublishingContext();
        context.setEntityId(entityId);
        context.setAuth("admin");
        context.setContent("dskladhjakl");
        ContentPublishingStatusEnum statusEnum = ContentPublishingStatusEnum.DRAFT;
        ContentPublishingEventEnum event = ContentPublishingEventEnum.SUBMIT;
        TXWorkflowBuilder.of(build)
                .entityId(contentPublishingStateMachine.machineId() + entityId)
                .fireEvent(statusEnum, event, context);
    }
}
