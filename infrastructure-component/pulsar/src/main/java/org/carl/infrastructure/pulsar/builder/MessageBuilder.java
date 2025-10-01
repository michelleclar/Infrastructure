package org.carl.infrastructure.pulsar.builder;

import java.util.Map;

/**
 * 消息构建器接口
 *
 * <p>使用构建器模式创建消息，支持设置消息内容、分区键、属性、延迟等配置
 *
 * <p>特性：
 *
 * <ul>
 *   <li>流式API，支持链式调用
 *   <li>支持分区键设置，用于消息路由和顺序保证
 *   <li>支持自定义属性，用于消息过滤和路由
 *   <li>支持延迟发送配置
 *   <li>支持事件时间设置
 * </ul>
 *
 * @param <T> 消息类型
 */
public interface MessageBuilder<T> {
    MessageBuilder<T> messageId(String messageId);

    MessageBuilder<T> topic(String topic);

    /**
     * 设置消息内容
     *
     * @param value 消息内容
     * @return 消息构建器，支持链式调用
     */
    MessageBuilder<T> value(T value);

    /**
     * 设置分区键
     *
     * <p>分区键用于：
     *
     * <ul>
     *   <li>消息路由：决定消息发送到哪个分区
     *   <li>顺序保证：相同键的消息按发送顺序被消费
     *   <li>负载均衡：不同键的消息分散到不同分区
     *   <li>消费者分组：Key_Shared模式下相同键由同一消费者处理
     * </ul>
     *
     * @param key 分区键，建议使用业务相关的稳定标识（如用户ID、订单ID）
     * @return 消息构建器，支持链式调用
     */
    MessageBuilder<T> key(String key);

    /**
     * 设置消息属性
     *
     * <p>消息属性用于：
     *
     * <ul>
     *   <li>消息过滤：消费者可根据属性过滤消息
     *   <li>消息路由：根据属性进行条件路由
     *   <li>元数据传递：传递业务相关的元信息
     * </ul>
     *
     * @param properties 属性映射，键值对形式的元数据
     * @return 消息构建器，支持链式调用
     */
    MessageBuilder<T> properties(Map<String, String> properties);

    /**
     * 设置单个消息属性
     *
     * @param key 属性键
     * @param value 属性值
     * @return 消息构建器，支持链式调用
     */
    MessageBuilder<T> property(String key, String value);

    /**
     * 设置事件时间
     *
     * <p>事件时间表示消息所代表事件的实际发生时间，用于：
     *
     * <ul>
     *   <li>时间窗口计算
     *   <li>消息排序
     *   <li>延迟处理检测
     * </ul>
     *
     * @param timestamp 事件时间戳（毫秒）
     * @return 消息构建器，支持链式调用
     */
    MessageBuilder<T> eventTime(long timestamp);

    /**
     * 设置消息序列ID
     *
     * <p>用于消息去重和顺序保证
     *
     * @param sequenceId 序列ID
     * @return 消息构建器，支持链式调用
     */
    MessageBuilder<T> sequenceId(long sequenceId);

    /**
     * 设置延迟发送时间
     *
     * <p>消息将在指定时间后才被消费者接收
     *
     * @param delayMillis 延迟时间（毫秒）
     * @return 消息构建器，支持链式调用
     */
    MessageBuilder<T> deliverAfter(long delayMillis);

    /**
     * 设置定时发送时间
     *
     * <p>消息将在指定时间点被消费者接收
     *
     * @param timestamp 发送时间戳（毫秒）
     * @return 消息构建器，支持链式调用
     */
    MessageBuilder<T> deliverAt(long timestamp);

    /**
     * 禁用复制
     *
     * <p>消息不会被复制到其他数据中心（仅适用于支持地理复制的消息系统）
     *
     * @return 消息构建器，支持链式调用
     */
    MessageBuilder<T> disableReplication();

    /**
     * 获取消息内容
     *
     * @return 消息内容
     */
    T getValue();

    /**
     * 获取分区键
     *
     * @return 分区键，如果未设置则返回null
     */
    String getKey();

    /**
     * 获取消息属性
     *
     * @return 属性映射，如果未设置则返回空Map
     */
    Map<String, String> getProperties();

    /**
     * 获取指定属性值
     *
     * @param key 属性键
     * @return 属性值，如果不存在则返回null
     */
    String getProperty(String key);

    /**
     * 获取事件时间
     *
     * @return 事件时间戳（毫秒），如果未设置则返回0
     */
    long getEventTime();

    /**
     * 获取序列ID
     *
     * @return 序列ID，如果未设置则返回-1
     */
    long getSequenceId();

    /**
     * 获取延迟发送时间
     *
     * @return 延迟时间（毫秒），如果未设置则返回0
     */
    long getDeliverAfter();

    /**
     * 获取定时发送时间
     *
     * @return 发送时间戳（毫秒），如果未设置则返回0
     */
    long getDeliverAt();

    /**
     * 是否禁用复制
     *
     * @return 是否禁用复制
     */
    boolean isReplicationDisabled();

    /**
     * 检查是否有分区键
     *
     * @return 是否设置了分区键
     */
    boolean hasKey();

    /**
     * 检查是否有属性
     *
     * @return 是否设置了属性
     */
    boolean hasProperties();

    /**
     * 构建消息
     *
     * <p>完成消息构建，返回不可变的消息对象
     *
     * @return 构建完成的消息
     */
    Message<T> build();

    boolean hasDeliverAfter();

    boolean hasDeliverAt();

    boolean hasSequenceId();

    boolean hasEventTime();

    /**
     * 消息接口
     *
     * <p>不可变的消息对象，包含所有消息信息
     *
     * <p>TODO: 如果适配多个消息队列中间件,可能需要一个消息类型标志位
     *
     * @param <T> 消息类型
     */
    interface Message<T> {
        T getValue();

        Object getSourceMessage();

        String getMessageId();

        String getTopic();

        String getKey();

        Map<String, String> getProperties();

        String getProperty(String key);

        long getEventTime();

        long getSequenceId();

        boolean hasKey();

        boolean hasProperties();

        boolean hasSequenceId();

        boolean hasEventTime();
    }
}
