package com.kevin.mqclient.entry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Broker的信息：路由信息+仓库信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BrokerRoutingInfo {
	@EqualsAndHashCode.Exclude
	String ip;
	@EqualsAndHashCode.Exclude
	Integer port;
	String describe;

	/**
	 * Broker的仓库信息：{@code map<topic,List<QueueId>>}
	 */
	Map<String, List<Integer>> topicInfo;
}
