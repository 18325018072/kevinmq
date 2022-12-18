package com.kevin.broker.entry.pac;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * consumer 订阅信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsumerSubscribeInfoPack {
	private Map<String, String> subInfoMap;
	private Long pullBatchSize;
}
