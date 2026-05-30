package org.carl.infrastructure.workflow.engine;

import io.temporal.activity.Activity;
import io.temporal.activity.DynamicActivity;
import io.temporal.common.converter.EncodedValues;

import org.carl.infrastructure.workflow.api.ProcessRegistry;

/**
 * The one generic Temporal activity. Dispatches by activity-type name to the business
 * {@link org.carl.infrastructure.workflow.api.WorkflowActivity}.
 *
 * <h3>Naming scheme</h3>
 *
 * <p>Activity-type names on the wire are namespaced:
 * <ul>
 *   <li>{@code processId + "::" + activityName} → routes to
 *       {@link org.carl.infrastructure.workflow.api.WorkflowActivity#run}.
 *   <li>{@code processId + "::" + activityName + "#compensate"} → routes to
 *       {@link org.carl.infrastructure.workflow.api.WorkflowActivity#compensate}.
 * </ul>
 * Both variants are registered in {@link ProcessRegistry} at startup; the {@code ActivityBinding}
 * knows which branch to call. Runs outside workflow context, so IO is fine.
 */
public class GenericActivity implements DynamicActivity {

    @Override
    public Object execute(EncodedValues args) {
        // The activity-type name is the full namespaced name (processId::activityName[#compensate])
        String activityType = Activity.getExecutionContext().getInfo().getActivityType();
        ProcessRegistry.ActivityBinding binding = ProcessRegistry.activity(activityType);
        Object from = args.get(0, binding.stateType());
        Object to = args.get(1, binding.stateType());
        Object event = args.get(2, binding.eventType());
        Object ctx = args.get(3, binding.ctxType());
        binding.execute(from, to, event, ctx);
        return null;
    }
}
