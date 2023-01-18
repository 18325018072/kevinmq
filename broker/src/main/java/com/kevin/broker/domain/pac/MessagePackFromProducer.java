package com.kevin.broker.domain.pac;

import com.kevin.kevinmq.common.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * producer 投递的消息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessagePackFromProducer {
	private Message message;
	private String producerName;
}
