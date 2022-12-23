package com.kevin.broker.entry;

import com.kevin.kevinmq.common.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ConsumeQueue （逻辑消费队列）作为消费消息的索引，保存了指定 Topic 下的队列消息在 CommitLog 中的起始物理偏移量 offset ，
 * 消息大小 size 和消息 Tag 的 HashCode 值。 Consumer 即可根据 ConsumeQueue 来查找待消费的消息。
 *
 * @author Kevin2<br />
 * last updated:2022/5/16
 */
@Data
@AllArgsConstructor@NoArgsConstructor
public class MessageQueue {
	/**
	 * broker名 + topic名 + queue序号
	 */
	private Integer brokerId;
	private String topic;
	private Integer queueId;
	private List<Message> data;

	public void addMessage(Message msg) {
		data.add(msg);
	}

	public void removeMessage(Message message){
		data.remove(message);
	}

	/**
	 * 初始化消息的消费状态
	 */
	public void resetMessageConsumeStatus(Message message){
		for (Message m : data) {
			if (m.equals(message)) {
				m.getConsumeStatus().set(0);
			}
		}
	}
}
