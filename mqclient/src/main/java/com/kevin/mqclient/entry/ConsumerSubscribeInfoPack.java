package com.kevin.mqclient.entry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * consumer 订阅信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsumerSubscribeInfoPack {
	private Map<String, List<String>> subInfoMap;
	private Long pullBatchSize;
}
