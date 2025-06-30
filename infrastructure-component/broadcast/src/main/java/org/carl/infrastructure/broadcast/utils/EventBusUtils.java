package org.carl.infrastructure.broadcast.utils;

import io.vertx.core.eventbus.DeliveryOptions;

import org.jboss.logging.Logger;

import java.util.Set;

public class EventBusUtils {

    private static final Logger log = Logger.getLogger(EventBusUtils.class);

    /** {@link io.vertx.core.eventbus.MessageCodec} */
    private static final Set<Class<?>> defaultSupportTypes =
            Set.of(
                    String.class,
                    Boolean.class,
                    Byte.class,
                    Short.class,
                    Integer.class,
                    Long.class,
                    Float.class,
                    Double.class,
                    io.vertx.core.buffer.Buffer.class,
                    io.vertx.core.json.JsonObject.class,
                    io.vertx.core.json.JsonArray.class);

    public static boolean isDefaultSupportType(Class<?> clazz) {
        if (clazz == null) {
            return true;
        }
        return defaultSupportTypes.contains(clazz);
    }

    public static DeliveryOptions getIsBaseTypeDeliveryOptions(Class<?> clazz) {
        return isDefaultSupportType(clazz)
                ? IS_TRUE_BASE_TYPE_DELIVERY_OPTIONS
                : IS_FALSE_BASE_TYPE_DELIVERY_OPTIONS;
    }

    public static DeliveryOptions IS_TRUE_BASE_TYPE_DELIVERY_OPTIONS;
    public static DeliveryOptions IS_FALSE_BASE_TYPE_DELIVERY_OPTIONS;

    static {
        IS_TRUE_BASE_TYPE_DELIVERY_OPTIONS =
                new DeliveryOptions() {
                    {
                        addHeader("isBaseType", "true");
                    }
                };
        IS_FALSE_BASE_TYPE_DELIVERY_OPTIONS =
                new DeliveryOptions() {
                    {
                        addHeader("isBaseType", "false");
                    }
                };
        if (log.isDebugEnabled()) {
            log.debugf("Broadcast :init default sport types [%s]", defaultSupportTypes);
        }
    }
}
