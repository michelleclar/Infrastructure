package org.carl.infrastructure.ability;

import org.carl.infrastructure.utils.LogUtils;
import org.jboss.logging.Logger;

public interface IGrpcClientLogAbility {

    Logger LOGGER = Logger.getLogger("grpc.log");

    default void grpcClientSuccess(Object o) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugf(
                    "%s success: %s", grpcClientName(), LogUtils.toShortString(o, logLength()));
        }
    }

    default void grpcClientFailure(Throwable throwable) {
        LOGGER.errorf("%s failure: %s", grpcClientName(), throwable.getMessage());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.errorf(throwable, "%s failure: %s", grpcClientName(), throwable.getMessage());
        }
    }

    default String grpcClientName() {
        return "grpc.client";
    }

    default int logLength() {
        return 100;
    }
}
