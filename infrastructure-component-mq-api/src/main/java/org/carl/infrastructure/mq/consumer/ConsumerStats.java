package org.carl.infrastructure.mq.consumer;

/** 消费者统计信息 */
public interface ConsumerStats {
    /**
     * 获取接收消息总数
     *
     * @return 接收消息总数
     */
    long getTotalReceivedMessages();

    /**
     * 获取接收字节总数
     *
     * @return 接收字节总数
     */
    long getTotalReceivedBytes();

    /**
     * 获取确认消息总数
     *
     * @return 确认消息总数
     */
    long getTotalAckedMessages();

    /**
     * 获取否定确认消息总数
     *
     * @return 否定确认消息总数
     */
    long getTotalNackedMessages();

    /**
     * 获取平均消费延迟（毫秒）
     *
     * @return 平均消费延迟
     */
    double getAverageConsumeLatency();

    /**
     * 获取当前未确认消息数
     *
     * @return 未确认消息数
     */
    int getUnackedMessages();

    /**
     * 获取最后接收时间
     *
     * @return 最后接收时间戳
     */
    long getLastReceivedTimestamp();

    /**
     * 获取最后确认时间
     *
     * @return 最后确认时间戳
     */
    long getLastAckedTimestamp();
}
