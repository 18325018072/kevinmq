package com.kevin.kevinmq.common;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@NoArgsConstructor
public class MessageBody implements Serializable {
	/**
	 * 消息ID
	 */
	@EqualsAndHashCode.Include
	@TableId(type = IdType.AUTO)
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

	public MessageBody(String topic, String tag, String body) {
		this.topic = topic;
		this.tag = tag;
		this.body = body;
	}

	public MessageBody(String topic, String body) {
		this.topic = topic;
		this.body = body;
	}
}
