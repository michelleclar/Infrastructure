package org.carl.infrastructure.broadcast;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

@QuarkusTest
public class EventBusDemo {
    @ConsumeEvent("greeting")
    public String greeting(String name) {
        return "Hello " + name;
    }
    @ConsumeEvent("pojo")
    public TransferEvent pojo(String name) {
        return new TransferEvent().setFrom(name + " from").setTo(name + " to").setAmount(BigDecimal.TWO);
    }

    @Inject EventBus eventBus;

    @Test
    void test() {

        eventBus.request("pojo", "hello").subscribe().with(item->{
            System.out.println(item.body());
        });
        eventBus.request("greeting", "hello").subscribe().with(item->{
            System.out.println(item.body());
        });
    }
}
