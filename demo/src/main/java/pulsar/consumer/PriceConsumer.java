package pulsar.consumer;

import io.smallrye.reactive.messaging.pulsar.PulsarIncomingMessageMetadata;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class PriceConsumer {

    //     @Incoming 注解将此方法标记为从名为 "prices" 的通道接收消息
    //    @Incoming("prices")
    //    public void consumePrice(double price) {
    //        // 直接处理 Double 类型的消息载荷
    //        System.out.println("Received price (payload only): " + price);
    //    }

    @Incoming("topic-1")
    public void consumeTopic1(String msg) {
        System.out.println("Received price (payload only): " + msg);
    }

    @Incoming("prices")
    public CompletionStage<Void> consumeMessage(Message<Double> msg) {
        // 访问消息元数据
        PulsarIncomingMessageMetadata metadata =
                msg.getMetadata(PulsarIncomingMessageMetadata.class).orElseThrow();
        // 处理消息载荷
        double price = msg.getPayload();

        System.out.println(
                "Received message (with metadata) - Price: "
                        + price
                        + ", Topic: "
                        + metadata.getTopicName());

        // 手动确认传入消息 (将 Pulsar 消息确认回 broker)
        // 如果方法接收 `Message<T>` 类型，则需要手动确认 [15]
        return msg.ack();
    }
}
