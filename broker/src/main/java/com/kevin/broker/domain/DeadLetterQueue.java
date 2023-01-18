package com.kevin.broker.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

import com.kevin.kevinmq.common.Message;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 存储死信消息的特殊队列(消费失败达到最大重试次数)
 * @TableName dead_letter_queue
 */
@TableName(value ="dead_letter_queue")
@Data
@NoArgsConstructor
public class DeadLetterQueue implements Serializable {
    /**
     * 消息ID
     */
    @TableId(type = IdType.AUTO)
    private Long messageId;

    public DeadLetterQueue(Message message) {
        this.messageId = message.getMessageId();
        this.topic = message.getTopic();
        this.tag = message.getTag();
        this.body = message.getBody();
        this.queueId = message.getQueueId();
        this.flag = message.getFlag();
    }

    /**
     * 主题
     */
    private String topic;

    /**
     * 标签
     */
    private String tag;

    /**
     * 主体
     */
    private String body;

    /**
     * 被分配到的消息队列ID
     */
    private Integer queueId;

    /**
     * 给应用使用
     */
    private Integer flag;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        DeadLetterQueue other = (DeadLetterQueue) that;
        return (this.getMessageId() == null ? other.getMessageId() == null : this.getMessageId().equals(other.getMessageId()))
            && (this.getTopic() == null ? other.getTopic() == null : this.getTopic().equals(other.getTopic()))
            && (this.getTag() == null ? other.getTag() == null : this.getTag().equals(other.getTag()))
            && (this.getBody() == null ? other.getBody() == null : this.getBody().equals(other.getBody()))
            && (this.getQueueId() == null ? other.getQueueId() == null : this.getQueueId().equals(other.getQueueId()))
            && (this.getFlag() == null ? other.getFlag() == null : this.getFlag().equals(other.getFlag()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getMessageId() == null) ? 0 : getMessageId().hashCode());
        result = prime * result + ((getTopic() == null) ? 0 : getTopic().hashCode());
        result = prime * result + ((getTag() == null) ? 0 : getTag().hashCode());
        result = prime * result + ((getBody() == null) ? 0 : getBody().hashCode());
        result = prime * result + ((getQueueId() == null) ? 0 : getQueueId().hashCode());
        result = prime * result + ((getFlag() == null) ? 0 : getFlag().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", messageId=").append(messageId);
        sb.append(", topic=").append(topic);
        sb.append(", tag=").append(tag);
        sb.append(", body=").append(body);
        sb.append(", queueId=").append(queueId);
        sb.append(", flag=").append(flag);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}