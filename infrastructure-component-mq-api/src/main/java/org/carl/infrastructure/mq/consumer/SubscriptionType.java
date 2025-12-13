package org.carl.infrastructure.mq.consumer;

/** 订阅类型枚举 */
public enum SubscriptionType {
    EXCLUSIVE, // 独占订阅
    SHARED, // 共享订阅
    FAILOVER, // 故障转移订阅
    KEY_SHARED // 按键共享订阅
}
