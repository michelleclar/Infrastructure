package org.carl.infrastructure.broadcast;

import io.smallrye.mutiny.Uni;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.MessageConsumer;

import org.jboss.logging.Logger;

import java.util.List;
import java.util.function.Function;

/** now use eventbus driven message queue TODO: need design topic rule */
public interface IBroadcastOperations extends IBroadcastProvider {
    BroadcastRegistry registry();

    Logger log = Logger.getLogger(IBroadcastOperations.class);

    /**
     * @param topic topic sing
     * @param message any
     * @return fluent
     */
    default <T> EventBus publish(String topic, T message) {
        if (log.isDebugEnabled()) {
            log.debugf("Publish: message to topic [%s],message [%s]", topic, message);
        }
        return getBroadcastContext().getEventBus().publish(topic, message, new DeliveryOptions());
    }

    default <T> EventBus publish(String topic, T message, DeliveryOptions options) {
        return getBroadcastContext().getEventBus().publish(topic, message, options);
    }

    default <T> Uni<Object> request(String topic, T message, DeliveryOptions options) {
        if (log.isDebugEnabled()) {
            log.debugf(
                    "Request: message to topic [%s],message [%s],options [%s]",
                    topic, message, options);
        }
        return getBroadcastContext()
                .getEventBus()
                .request(topic, message, options)
                .onItem()
                .transform(
                        msg -> {
                            if (log.isDebugEnabled()) {
                                log.debugf(
                                        "Request: message [%s] to topic [%s] response [%s] options [%s]",
                                        message, topic, msg.body(), options);
                            }
                            return msg.body();
                        });
    }

    default <T> Uni<Object> request(String topic, T message) {
        return getBroadcastContext()
                .getEventBus()
                .request(topic, message)
                .onItem()
                .transform(
                        msg -> {
                            Object body = msg.body();
                            if (log.isDebugEnabled()) {
                                log.debugf(
                                        "Request: message [%s] to topic [%s] response [%s]",
                                        message, topic, msg.body());
                            }
                            return body;
                        });
    }

    default <R> Uni<R> request(String topic, Object message, Class<R> ignore) {
        return getBroadcastContext()
                .getEventBus()
                .<R>request(topic, message)
                .onItem()
                .transform(
                        msg -> {
                            R body = msg.body();
                            if (log.isDebugEnabled()) {
                                log.debugf(
                                        "Request: message [%s] to topic [%s] response [%s]",
                                        message, topic, msg.body());
                            }
                            return body;
                        });
    }

    default <Req, Res> Uni<Void> respond(
            String topic,
            Class<Req> reqClass,
            Class<Res> resClass,
            Function<Req, Uni<Res>> responder,
            boolean isObservable) {
        EventBus eventBus = getBroadcastContext().getEventBus();
        if (log.isDebugEnabled()) {
            log.debugf("Respond: topic [%s] Observable [%s]", topic, isObservable);
        }

        if (isObservable && registry().has(topic)) {
            if (log.isDebugEnabled()) {
                log.debug("Respond: topic exists");
            }
            return Uni.createFrom().voidItem();
        }

        MessageConsumer<Req> consumer =
                eventBus.consumer(
                        topic,
                        msg -> {
                            Req body = msg.body();
                            responder
                                    .apply(body)
                                    .subscribe()
                                    .with(
                                            response -> {
                                                if (log.isDebugEnabled()) {
                                                    log.debugf("Respond: response [%s]", response);
                                                }
                                                msg.reply(response);
                                            },
                                            err -> {
                                                log.errorf(
                                                        "Respond: Failed to process response for topic [%s] error message [%s]",
                                                        topic, err.getMessage());
                                                if (log.isDebugEnabled()) {
                                                    log.errorf(
                                                            "Respond: exception stack [%s]",
                                                            err.getMessage());
                                                }
                                                msg.fail(500, err.getMessage());
                                            });
                        });

        if (!isObservable) {
            return Uni.createFrom().voidItem();
        }
        if (log.isDebugEnabled()) {
            log.debugf("Respond: Registered responder for topic [%s]", topic);
        }

        registry().put(topic, consumer);
        return Uni.createFrom().voidItem();
    }

    default <T> Uni<Void> subscribe(
            String topic, Class<T> ignore, java.util.function.Consumer<T> handler) {
        EventBus eventBus = getBroadcastContext().getEventBus();

        return unsubscribe(topic)
                .onItem()
                .transform(
                        __ -> {
                            MessageConsumer<T> consumer =
                                    eventBus.<T>consumer(topic)
                                            .handler(
                                                    msg -> {
                                                        handler.accept(msg.body());
                                                    });
                            registry().put(topic, consumer);
                            if (log.isDebugEnabled()) {
                                log.debugf("Subscribe: Subscribed to topic [%s]", topic);
                            }
                            return __;
                        });
    }

    default Uni<Void> unsubscribe(String topic) {

        MessageConsumer<?> consumer = registry().remove(topic);
        if (consumer != null) {
            return consumer.unregister();
        }
        return Uni.createFrom().voidItem();
    }

    default Uni<Void> unsubscribeAll() {
        List<Uni<Void>> tasks = registry().all().stream().map(MessageConsumer::unregister).toList();
        registry().clear();
        return Uni.combine().all().unis(tasks).with(list -> null);
    }
}
