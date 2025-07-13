package workflow.publish;

import com.fasterxml.jackson.databind.JsonMappingException;

import io.quarkus.arc.ArcUndeclaredThrowableException;
import io.temporal.activity.ActivityCancellationType;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import org.carl.infrastructure.statemachine.Action;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Provider class for creating activity stubs with predefined options. This class sets up common
 * activity options and provides methods to create activity stubs for various services in the pet
 * store application.
 */
public class ActivityStubsProvider {
    public static <S, E, C> Action<S, E, C> wrapper(Consumer<C> consumer) {
        return (from, to, event, ctx) -> {
            // log begin
            // do something
            // log after
            consumer.accept(ctx);
        };
    }

    // Activity options for demo purposes, limiting retry time
    private static final ActivityOptions options =
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(
                            RetryOptions.newBuilder()
                                    .setDoNotRetry(
                                            ArcUndeclaredThrowableException.class.getName(),
                                            JsonMappingException.class.getName(),
                                            NullPointerException.class.getName(),
                                            IllegalArgumentException.class.getName())
                                    .setInitialInterval(Duration.ofSeconds(1))
                                    .setMaximumInterval(Duration.ofSeconds(100))
                                    .setBackoffCoefficient(2)
                                    .setMaximumAttempts(500)
                                    .build())
                    .build();

    public static ContentPublishingActivity getWarehouseActivities() {
        ActivityOptions newOptions =
                ActivityOptions.newBuilder(options)
                        .setTaskQueue(ContentPublishingWorkflowImpl.machineId)
                        .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
                        .build();

        return Workflow.newActivityStub(ContentPublishingActivity.class, newOptions);
    }
}
