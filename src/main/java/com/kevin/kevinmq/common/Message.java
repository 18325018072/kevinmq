package com.kevin.kevinmq.common;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class Message extends MessageBody {
	private final static int MAX_RETRY_COUNTDOWN=10;
	/**
	 * 消费状态：0未消费，1消费中，2已消费
	 */
	@TableField(exist = false)
	private AtomicInteger consumeStatus = new AtomicInteger(0);

	/**
	 * 重试倒计时。<br/>
	 * 发送消息后，在被确认消费成功或确认失败前，每被跳过一次，则减一。为负数后，认为消费失败。
	 */
	private int retryCountdown=MAX_RETRY_COUNTDOWN;

	/**
	 * 重试次数
	 */
	@TableField(exist = false)
	private int retryTime = 0;

	@TableField(exist = false)
	private static final long serialVersionUID = 1L;

	/**
	 * 重试倒计时减一。
	 */
	public void subtractCountdown(){
		retryCountdown--;
	}

	public Message(String topic, String tag, String body) {
		super(topic,tag,body);
	}

	public Message(String topic, String body) {
		super(topic,body);
	}

	/**
	 * 重试。<br/>
	 * 重试次数+1，重置重试倒计时，还原消费状态。
	 */
	public void retry() {
		retryTime++;
		retryCountdown=MAX_RETRY_COUNTDOWN;
		consumeStatus.set(0);
	}
}
