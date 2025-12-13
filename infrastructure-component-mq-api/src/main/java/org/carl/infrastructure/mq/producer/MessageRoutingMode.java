package org.carl.infrastructure.mq.producer;

public enum MessageRoutingMode {
    /**
     * If no key is provided, The partitioned producer will randomly pick one single partition and
     * publish all the messages into that partition. If a key is provided on the message, the
     * partitioned producer will hash the key and assign message to a particular partition.
     */
    SinglePartition,

    /**
     * If no key is provided, the producer will publish messages across all partitions in
     * round-robin fashion to achieve maximum throughput. Please note that round-robin is not done
     * per individual message but rather it's set to the same boundary of batching delay, to ensure
     * batching is effective.
     *
     * <p>While if a key is specified on the message, the partitioned producer will hash the key and
     * assign message to a particular partition.
     */
    RoundRobinPartition,

    /**
     * Use custom message router implementation that will be called to determine the partition for a
     * particular message.
     */
    CustomPartition
}
