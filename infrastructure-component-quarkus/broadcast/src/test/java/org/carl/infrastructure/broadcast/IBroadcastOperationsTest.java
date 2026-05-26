package org.carl.infrastructure.broadcast;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

@QuarkusTest
class IBroadcastOperationsTest extends BroadcastStd {

    @BeforeEach
    void setUp() {
        subscribe("test.int", Integer.class, __ -> {}).subscribe().with(__ -> {});
        subscribe("test.string", String.class, __ -> {}).subscribe().with(__ -> {});
        subscribe("test.transfer", TransferEvent.class, __ -> {}).subscribe().with(__ -> {});
        subscribe("test.unsubscribe", Integer.class, __ -> {}).await().indefinitely();
    }

    @Test
    void publishTest() {
        TransferEvent transferEvent =
                new TransferEvent().setFrom("a").setTo("b").setAmount(BigDecimal.TWO);
        publish("test.int", 1)
                .publish("test.string", "event...")
                .publish("test.transfer", transferEvent);
    }

    @Test
    void request() {
        request("test.int", 1).subscribe().with(__ -> {});
    }

    @Test
    void respond() {
        String topic = "test.respond";
        respond(
                        topic,
                        Integer.class,
                        String.class,
                        res -> {
                            res++;
                            return Uni.createFrom().item(res.toString());
                        },
                        false)
                .await()
                .indefinitely();
        request(topic, 1).subscribe().with(__ -> {});
    }

    @Test
    void unsubscribe() {
        String topic = "test.unsubscribe";
        request(topic, 1).subscribe().with(__ -> {});
        unsubscribe(topic).await().indefinitely();
        request(topic, 1).subscribe().with(__ -> {});
    }

    @Test
    void unsubscribeAllTest() {
        publishTest();
        unsubscribeAll().await().indefinitely();
        publishTest();
    }
}
