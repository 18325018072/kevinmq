package com.kevin.mqclient.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevin.kevinmq.common.BaseResponsePack;
import com.kevin.kevinmq.common.BrokerRoutingInfo;
import com.kevin.mqclient.entry.ConsumerSubscribeInfoPack;
import com.kevin.kevinmq.common.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.BiConsumer;

@Repository
public class HttpUtil {
	@Autowired
	private RestTemplate restTemplate;
	private final String HTTP_PREFIX = "http://";

	/**
	 * 测试能否调通url
	 */
	public boolean testUrl(String url) {
		ResponseEntity<BaseResponsePack> response = restTemplate.getForEntity(url + "/testNameServer", BaseResponsePack.class);
		BaseResponsePack responseBody = response.getBody();
		return responseBody != null && responseBody.getStatus() == BaseResponsePack.SUCCESS_CODE;
	}

	/**
	 * 消费者：从 Broker 拉取消息
	 */
	public List<Message> pullFromBroker(BrokerRoutingInfo targetBrokerRoutingInfo, Map<String, List<String>> subscriptionMap, long pullBatchSize) {
		String url = HTTP_PREFIX + targetBrokerRoutingInfo.getIp() + ":" + targetBrokerRoutingInfo.getPort() + "/getMessageBatch";
		ConsumerSubscribeInfoPack request = new ConsumerSubscribeInfoPack(subscriptionMap, pullBatchSize);
		BaseResponsePack response = restTemplate.postForEntity(url, request, BaseResponsePack.class).getBody();
		if (response.getStatus() != 0) {
			return null;
		}
		List<Map<String, Object>> list = (List<Map<String, Object>>) response.getObject();
		if (list.isEmpty()){
			return null;
		}
		List<Message> res = new ArrayList<>();
		for (Map<String, Object> map : list) {
			Message message = new Message((String) map.get("topic"), (String) map.get("tag"), (String) map.get("body"));
			message.setQueueId((Integer) map.get("queueId"));
			message.setMessageId((Long) map.get("messageId"));
			message.setFlag((Integer) map.get("flag"));
			res.add(message);
		}
		return res;
	}

	/**
	 * 消费者/生产者：查询 topic 对应的 broker
	 */
	public Map<String, List<BrokerRoutingInfo>> getBrokersByTopics(Set<String> keySet, String nameServerUrl) {
		StringBuilder stringBuilder = new StringBuilder(nameServerUrl + "/getBrokerInfoByTopic?");
		for (String topic : keySet) {
			stringBuilder.append("topicSet=").append(topic).append("&");
		}
		String url = stringBuilder.substring(0, stringBuilder.length() - 1);
		BaseResponsePack response = restTemplate.getForEntity(url, BaseResponsePack.class).getBody();
		if (response.getStatus() != 0) {
			throw new RuntimeException("getBrokersByTopics:" + response.getInfo());
		}
		Map<String, List<Map<String, Object>>> obj = (Map<String, List<Map<String, Object>>>) response.getObject();
		Map<String, List<BrokerRoutingInfo>> res = new HashMap<>(5);
		obj.forEach((s, maps) -> {
			List<BrokerRoutingInfo> brokerList = new ArrayList<>();
			for (Map<String, Object> map : maps) {
				brokerList.add(new BrokerRoutingInfo((String) map.get("ip"), (Integer) map.get("port"), (String) map.get("describe"), (Map<String, List<Integer>>) map.get("topicInfo")));
			}
			res.put(s, brokerList);
		});
		return res;
	}

	/**
	 * 生产者：发送消息
	 */
	public BaseResponsePack sendSynchronously(Message message, BrokerRoutingInfo targetBrokerRoutingInfo, String producerName) {
		String url = HTTP_PREFIX + targetBrokerRoutingInfo.getIp() + ":" + targetBrokerRoutingInfo.getPort() + "/postMessage";
		Map<String, Object> request = new HashMap<>(2);
		request.put("message", message);
		request.put("producerName", producerName);
		return restTemplate.postForEntity(url, request, BaseResponsePack.class).getBody();
	}

	/**
	 * 消费者：反馈消费
	 */
	public void feedback(List<Message> resMsgList, BrokerRoutingInfo brokerRoutingInfo) {
		String url = HTTP_PREFIX + brokerRoutingInfo.getIp() + ":" + brokerRoutingInfo.getPort() + "/solveConsumerFeedback";
		BaseResponsePack response = restTemplate.postForEntity(url, resMsgList, BaseResponsePack.class).getBody();
		if (response.getStatus() != 0) {
			throw new RuntimeException(response.getInfo());
		}
	}
}
