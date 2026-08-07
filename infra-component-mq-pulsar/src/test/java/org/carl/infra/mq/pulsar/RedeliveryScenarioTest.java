package org.carl.infra.mq.pulsar;

import static org.junit.jupiter.api.Assertions.*;

import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.consumer.DeadLetterPolicy;
import org.carl.infra.mq.consumer.IConsumer;
import org.carl.infra.mq.consumer.SubscriptionInitialPosition;
import org.carl.infra.mq.model.Message;
import org.carl.infra.mq.producer.IProducer;
import org.carl.infra.mq.pulsar.builder.MQClientBuilder;
import org.carl.infra.mq.pulsar.config.PulsarConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Pulsar 消息重投与死信队列集成场景。
 *
 * <p>测试需要本地 Pulsar Broker，默认连接 {@code pulsar://127.0.0.1:6650}。每个场景使用独立
 * Topic、订阅和消息，避免不同测试之间共享消费位点。
 */
class RedeliveryScenarioTest {

    @BeforeEach
    void setUp() {
        System.setProperty("CLOUDEMQ_SERVICE_URL", "pulsar://127.0.0.1:6650");
    }

    /**
     * 场景一：关闭自动确认且不设置 Ack 超时。
     *
     * <p>消息首次投递后既不 Ack 也不 nack；在消费者保持连接期间，不应按固定周期重复投递。
     */
    @Test
    void defaultZeroAckTimeoutDoesNotRedeliverUnacknowledgedMessageOnFixedPeriod()
            throws Exception {
        String serviceUrl = serviceUrl();
        String runId = UUID.randomUUID().toString();
        String topic = "infra-cloudemq-no-ack-timeout-" + runId;
        String subscription = "infra-cloudemq-no-ack-timeout-sub-" + runId;
        String payload = "no-ack-timeout-" + runId;

        MQClient client = MQClientBuilder.createClient(new PulsarConfig(serviceUrl));
        IProducer<String> producer = null;
        IConsumer<String> consumer = null;
        try {
            consumer =
                    client.newConsumer(String.class)
                            .subscriptionName(subscription)
                            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                            .autoAck(false)
                            .subscribe(topic);
            producer = client.newProducer(String.class).create(topic);

            IProducer.SendResult<String> sendResult = producer.sendMessage(payload);
            assertTrue(sendResult.isSuccess(), sendResult.getErrorMessage());

            Message<String> first = consumer.receive(20, TimeUnit.SECONDS);
            assertNotNull(first, "首次投递应在 20 秒内到达");
            assertEquals(payload, first.getValue());

            // 当前默认 ackTimeout 为 Duration.ZERO；保持消费者连接且不确认时，不会按固定周期重投。
            Message<String> unexpectedRedelivery = consumer.receive(5, TimeUnit.SECONDS);
            assertNull(unexpectedRedelivery, "默认关闭 ackTimeout 时不应出现固定周期重投");
        } finally {
            closeResources(client, producer, consumer, null);
        }
    }

    /**
     * 场景二：关闭自动确认，设置 {@code ackTimeout=2s}，验证未确认消息在超时后重投。
     */
    @Test
    void ackTimeoutRedeliversSameMessageAfterConfiguredWindow() throws Exception {
        String serviceUrl = serviceUrl();
        String runId = UUID.randomUUID().toString();
        String topic = "infra-cloudemq-ack-timeout-" + runId;
        String subscription = "infra-cloudemq-ack-timeout-sub-" + runId;
        String payload = "ack-timeout-" + runId;

        MQClient client = MQClientBuilder.createClient(new PulsarConfig(serviceUrl));
        IProducer<String> producer = null;
        IConsumer<String> consumer = null;
        try {
            consumer =
                    client.newConsumer(String.class)
                            .subscriptionName(subscription)
                            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                            .autoAck(false)
                            .ackTimeout(2, TimeUnit.SECONDS)
                            .ackTimeoutTickTime(1, TimeUnit.SECONDS)
                            .subscribe(topic);
            producer = client.newProducer(String.class).create(topic);

            IProducer.SendResult<String> sendResult = producer.sendMessage(payload);
            assertTrue(sendResult.isSuccess(), sendResult.getErrorMessage());

            Message<String> first = consumer.receive(20, TimeUnit.SECONDS);
            assertNotNull(first, "首次投递应在 20 秒内到达");
            assertEquals(payload, first.getValue());

            // 首次消息不确认；用覆盖 2 秒 ackTimeout 和 1 秒 tick 的窗口等待重新投递。
            Message<String> redelivered = consumer.receive(10, TimeUnit.SECONDS);
            assertNotNull(redelivered, "未确认消息应在 ackTimeout 到期后重新投递");
            assertEquals(first.getMessageId(), redelivered.getMessageId());
            consumer.acknowledge(redelivered);
        } finally {
            closeResources(client, producer, consumer, null);
        }
    }

    /**
     * 场景三：主动 negative Ack，设置 1 秒重投延迟，验证同一消息再次投递。
     */
    @Test
    void negativeAcknowledgeRedeliversSameMessage() throws Exception {
        String serviceUrl = serviceUrl();
        String runId = UUID.randomUUID().toString();
        String topic = "infra-cloudemq-negative-ack-" + runId;
        String subscription = "infra-cloudemq-negative-ack-sub-" + runId;
        String payload = "negative-ack-" + runId;

        MQClient client = MQClientBuilder.createClient(new PulsarConfig(serviceUrl));
        IProducer<String> producer = null;
        IConsumer<String> consumer = null;
        try {
            consumer =
                    client.newConsumer(String.class)
                            .subscriptionName(subscription)
                            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                            .autoAck(false)
                            .negativeAckRedeliveryDelay(1, TimeUnit.SECONDS)
                            .subscribe(topic);
            producer = client.newProducer(String.class).create(topic);

            IProducer.SendResult<String> sendResult = producer.sendMessage(payload);
            assertTrue(sendResult.isSuccess(), sendResult.getErrorMessage());

            Message<String> first = consumer.receive(20, TimeUnit.SECONDS);
            assertNotNull(first, "首次投递应在 20 秒内到达");
            assertEquals(payload, first.getValue());

            // 主动否定确认，预期在 1 秒延迟后收到同一条消息。
            consumer.negativeAcknowledge(first);
            Message<String> redelivered = consumer.receive(10, TimeUnit.SECONDS);
            assertNotNull(redelivered, "negativeAcknowledge 后应重新投递消息");
            assertEquals(first.getMessageId(), redelivered.getMessageId());
            consumer.acknowledge(redelivered);
        } finally {
            closeResources(client, producer, consumer, null);
        }
    }

    /**
     * 场景四：不启用 retry-letter topic，仅通过连续 negative Ack 验证默认 DLQ。
     *
     * <p>不指定死信 Topic 名称时，Pulsar 使用 {@code <topic>-<subscription>-DLQ}。
     */
    @Test
    void negativeAcknowledgeMovesMessageToDefaultDeadLetterTopic() throws Exception {
        String serviceUrl = serviceUrl();
        String runId = UUID.randomUUID().toString();
        String topic = "infra-cloudemq-default-dlq-" + runId;
        String subscription = "infra-cloudemq-default-dlq-sub-" + runId;
        String dlqSubscription = "infra-cloudemq-default-dlq-initial-sub-" + runId;
        String deadLetterTopic = topic + "-" + subscription + "-DLQ";
        String payload = "default-dlq-" + runId;

        MQClient client = MQClientBuilder.createClient(new PulsarConfig(serviceUrl));
        IProducer<String> producer = null;
        IConsumer<String> consumer = null;
        IConsumer<String> dlqConsumer = null;
        try {
            consumer =
                    client.newConsumer(String.class)
                            .subscriptionName(subscription)
                            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                            .autoAck(false)
                            .negativeAckRedeliveryDelay(1, TimeUnit.SECONDS)
                            .deadLetterPolicy(
                                    DeadLetterPolicy.builder()
                                            .maxRedeliverCount(2)
                                            .initialSubscriptionName(dlqSubscription)
                                            .build())
                            .subscribe(topic);
            dlqConsumer =
                    client.newConsumer(String.class)
                            .subscriptionName(dlqSubscription)
                            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                            .autoAck(false)
                            .subscribe(deadLetterTopic);
            producer = client.newProducer(String.class).create(topic);

            IProducer.SendResult<String> sendResult = producer.sendMessage(payload);
            assertTrue(sendResult.isSuccess(), sendResult.getErrorMessage());

            Message<String> first = consumer.receive(20, TimeUnit.SECONDS);
            assertNotNull(first, "首次投递应在 20 秒内到达");
            assertEquals(payload, first.getValue());

            // 不开启 enableRetry；仅通过连续 negativeAcknowledge 验证消息进入默认 DLQ。
            Message<String> current = first;
            Message<String> dlqMessage = null;
            for (int negativeAckCount = 1; negativeAckCount <= 5; negativeAckCount++) {
                assertEquals(
                        first.getMessageId(),
                        current.getMessageId(),
                        "进入 DLQ 前每次都应是同一条原始消息");
                consumer.negativeAcknowledge(current);

                dlqMessage = dlqConsumer.receive(3, TimeUnit.SECONDS);
                if (dlqMessage != null) {
                    break;
                }

                current = consumer.receive(10, TimeUnit.SECONDS);
                if (current == null) {
                    dlqMessage = dlqConsumer.receive(10, TimeUnit.SECONDS);
                    break;
                }
            }

            assertNotNull(dlqMessage, "消息在最多 5 次 negativeAcknowledge 后仍未进入默认 DLQ");
            assertEquals(payload, dlqMessage.getValue());
            dlqConsumer.acknowledge(dlqMessage);
        } finally {
            closeResources(client, producer, consumer, dlqConsumer);
        }
    }

    /**
     * 场景五：配置 retry 开关、显式 retry Topic 和显式 DLQ Topic。
     *
     * <p>当前公共 IConsumer 未暴露 Pulsar 的 {@code reconsumeLater}，因此这里只验证配置可以
     * 创建消费者并完成普通投递，不断言消息已经写入 retry-letter topic。
     */
    @Test
    void explicitRetryAndDeadLetterTopicsConfigureConsumerWithoutClaimingRetryLetterDelivery()
            throws Exception {
        String serviceUrl = serviceUrl();
        String runId = UUID.randomUUID().toString();
        String topic = "infra-cloudemq-explicit-retry-" + runId;
        String subscription = "infra-cloudemq-explicit-retry-sub-" + runId;
        String retryLetterTopic = topic + "-retry";
        String deadLetterTopic = topic + "-dlq";
        String dlqSubscription = "infra-cloudemq-explicit-retry-dlq-sub-" + runId;
        String payload = "explicit-retry-" + runId;

        MQClient client = MQClientBuilder.createClient(new PulsarConfig(serviceUrl));
        IProducer<String> producer = null;
        IConsumer<String> consumer = null;
        try {
            consumer =
                    client.newConsumer(String.class)
                            .subscriptionName(subscription)
                            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                            .autoAck(false)
                            .enableRetry(true)
                            .deadLetterPolicy(
                                    DeadLetterPolicy.builder()
                                            .maxRedeliverCount(2)
                                            .retryLetterTopic(retryLetterTopic)
                                            .deadLetterTopic(deadLetterTopic)
                                            .initialSubscriptionName(dlqSubscription)
                                            .build())
                            .subscribe(topic);
            producer = client.newProducer(String.class).create(topic);

            IProducer.SendResult<String> sendResult = producer.sendMessage(payload);
            assertTrue(sendResult.isSuccess(), sendResult.getErrorMessage());

            // 当前公共 IConsumer 未暴露 reconsumeLater(message, delay)。这里只验证显式 retry/DLQ
            // 配置可创建消费者并完成普通投递，不声称消息已经写入 retry-letter topic。
            Message<String> received = consumer.receive(20, TimeUnit.SECONDS);
            assertNotNull(received, "显式 retry/DLQ 配置的消费者应能接收普通消息");
            assertEquals(payload, received.getValue());
            consumer.acknowledge(received);
        } finally {
            closeResources(client, producer, consumer, null);
        }
    }

    private static void closeResources(
            MQClient client,
            IProducer<String> producer,
            IConsumer<String> firstConsumer,
            IConsumer<String> secondConsumer)
            throws Exception {
        Exception failure = null;
        if (producer != null) {
            try {
                producer.closeAsync().get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                failure = e;
            }
        }
        if (firstConsumer != null) {
            try {
                firstConsumer.close();
            } catch (Exception e) {
                failure = appendFailure(failure, e);
            }
        }
        if (secondConsumer != null) {
            try {
                secondConsumer.close();
            } catch (Exception e) {
                failure = appendFailure(failure, e);
            }
        }
        try {
            client.close();
        } catch (Exception e) {
            failure = appendFailure(failure, e);
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static Exception appendFailure(Exception failure, Exception next) {
        if (failure == null) {
            return next;
        }
        failure.addSuppressed(next);
        return failure;
    }

    private static String serviceUrl() {
        return System.getProperty("CLOUDEMQ_SERVICE_URL");
    }
}
