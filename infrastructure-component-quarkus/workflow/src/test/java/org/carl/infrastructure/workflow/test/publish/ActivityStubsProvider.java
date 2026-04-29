package org.carl.infrastructure.workflow.test.publish;

import com.fasterxml.jackson.databind.JsonMappingException;

import io.quarkus.arc.ArcUndeclaredThrowableException;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;

import java.time.Duration;

/**
 * Provider class for creating activity stubs with predefined options. This class sets up common
 * activity options and provides methods to create activity stubs for various services in the pet
 * store application.
 */
public class ActivityStubsProvider {

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
}
