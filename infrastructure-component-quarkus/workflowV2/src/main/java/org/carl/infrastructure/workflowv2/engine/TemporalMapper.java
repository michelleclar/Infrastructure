package org.carl.infrastructure.workflowv2.engine;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;

import org.carl.infrastructure.workflowv2.api.RetryPolicy;

import java.time.Duration;

/** Maps vendor-neutral {@link RetryPolicy} / timeouts to Temporal options. */
final class TemporalMapper {

    private TemporalMapper() {}

    static RetryOptions toRetryOptions(RetryPolicy policy) {
        return RetryOptions.newBuilder()
                .setMaximumAttempts(policy.maxAttempts())
                .setInitialInterval(policy.initialInterval())
                .setBackoffCoefficient(policy.backoffCoefficient())
                .setMaximumInterval(policy.maximumInterval())
                .build();
    }

    static ActivityOptions activityOptions(Duration startToClose, RetryPolicy retry) {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(startToClose)
                .setRetryOptions(toRetryOptions(retry))
                .build();
    }
}
