package com.kevin.broker.entry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Message implements Serializable {
    /**
     * 消息的主题
     */
    @EqualsAndHashCode.Include
    private String topic;
    /**
     * 默认0。给应用使用。
     */
    private Integer flag;

    /**
     * 消息的标签
     */
    @EqualsAndHashCode.Include
    private String tags;

    /**
     * 消息的数据
     */
    private String body;

    /**
     * broker 对应队列ID
     */
    private Integer queueId;

    /**
     * 消息ID
     */
    @EqualsAndHashCode.Include
    private Long messageId;

    /**
     * 消费状态：0未消费，1消费中，2已消费
     */
    private AtomicInteger consumeStatus;
}
