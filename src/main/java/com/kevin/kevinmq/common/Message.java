package com.kevin.kevinmq.common;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消息
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Message implements Serializable {
	/**
	 * 消息ID
	 */
	@EqualsAndHashCode.Include
	@TableId
	private Long messageId;

	/**
	 * 消息的主题，必要的
	 */
	@EqualsAndHashCode.Include
	private String topic;
	/**
	 * 默认0。给应用使用。
	 */
	private Integer flag;

	/**
	 * 消息的标签。一条消息最多有一个tag
	 */
	private String tag;

	/**
	 * 消息的数据
	 */
	private String body;

	/**
	 * broker 对应队列ID
	 */
	private Integer queueId;

	/**
	 * 消费状态：0未消费，1消费中，2已消费
	 */
	@TableField(exist = false)
	private AtomicInteger consumeStatus = new AtomicInteger(0);

	@TableField(exist = false)
	private static final long serialVersionUID = 1L;

	public Message(String topic, String tag, String body) {
		this.topic = topic;
		this.tag = tag;
		this.body = body;
	}

	public Message(String topic, String body) {
		this.topic = topic;
		this.body = body;
	}
}
