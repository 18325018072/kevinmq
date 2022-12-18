package com.kevin.mqclient.entry.pac;

/**
 * 生产者发送消息的结果
 */
public enum SendStatus {
    /**
     * 发送成功
     */
    Send_OK,
    /**
     * Broker处理错误
     */
    Send_Error,
    /**
     * 没有对应该topic的broker
     */
    Broker_NotFound
}
