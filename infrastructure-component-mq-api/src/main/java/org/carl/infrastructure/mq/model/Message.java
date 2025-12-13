package org.carl.infrastructure.mq.model;

import java.util.Map;

/**
 * 消息接口
 *
 * <p>不可变的消息对象，包含所有消息信息
 *
 * <p>TODO: 如果适配多个消息队列中间件,可能需要一个消息类型标志位
 *
 * @param <T> 消息类型
 */
public interface Message<T> {
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
