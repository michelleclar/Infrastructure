package org.carl.infrastructure.workflow.runtime;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Single generic Temporal activity used by {@link GenericWorkflowImpl} to dispatch service-task
 * business functions registered through {@link BusinessActivityRegistry}.
 */
@ActivityInterface
public interface GenericActivity {

    @ActivityMethod
    ActivityResult execute(ActivityCall call);
}
