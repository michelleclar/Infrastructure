package pulsar.producer;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import java.util.Random;

@ApplicationScoped
public class PulsarPriceProducer {

    private final Random random = new Random();

    // @Outgoing 注解将此方法标记为生成消息到名为 "prices-out" 的通道
    @Outgoing("prices-out")
    public Double generate() {
        // 构建一个无限流，每秒发射一个随机价格
        return 0.1;
        //        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
        //                .map(x -> random.nextDouble() * 100); // 假设价格在 0 到 100 之间
    }
}
