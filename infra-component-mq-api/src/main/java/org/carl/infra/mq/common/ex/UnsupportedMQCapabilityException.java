package org.carl.infra.mq.common.ex;

import java.util.Objects;

/** Raised immediately when an MQ provider receives a capability it does not implement. */
public final class UnsupportedMQCapabilityException extends IllegalArgumentException {

    public UnsupportedMQCapabilityException(String provider, String capability, Object value) {
        super(
                "MQ provider '"
                        + Objects.requireNonNull(provider, "provider")
                        + "' does not support "
                        + Objects.requireNonNull(capability, "capability")
                        + ": "
                        + String.valueOf(value));
    }
}
