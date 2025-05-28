package org.carl.infrastructure.broadcast;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import io.vertx.mutiny.core.eventbus.MessageConsumer;
import jakarta.inject.Inject;
import org.carl.infrastructure.broadcast.core.BroadcastContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@ApplicationScoped
@IfBuildProperty(name = "quarkus.message.enable", stringValue = "true")
public class BroadcastService extends BroadcastStd implements IBroadcastOperations {

}
