package org.carl.infrastructure.mq.reader;

/** Reader 起始位置 */
public enum ReaderStartPosition {
    /** 从 topic 最早的消息开始读取 */
    Earliest,
    /** 从 topic 最新位置开始读取（仅读此后新投递的消息） */
    Latest
}
