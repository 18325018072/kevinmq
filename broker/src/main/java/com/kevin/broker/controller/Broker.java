package com.kevin.broker.controller;


import com.kevin.kevinmq.common.BrokerRoutingInfo;
import com.kevin.broker.domain.pac.ConsumerSubscribeInfoPack;
import com.kevin.broker.domain.pac.MessagePackFromProducer;
import com.kevin.broker.service.BrokerService;
import com.kevin.kevinmq.common.BaseResponsePack;
import com.kevin.kevinmq.common.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 服务器<br/>
 *
 * @author Kevin2
 */
@RestController
@CrossOrigin
public class Broker {
	BrokerService service;

	@Autowired
	public Broker(BrokerService service) {
		this.service = service;
	}

	@GetMapping("testBroker")
	public BaseResponsePack testUrl() {
		return new BaseResponsePack(BaseResponsePack.SUCCESS_CODE, null, service.getBrokerStatus());
	}

	/**
	 * 接收 生产者的消息
	 */
	@PostMapping("postMessage")
	public BaseResponsePack receiveMessage(@RequestBody MessagePackFromProducer messagePac) {
		//提取数据
		Message message = messagePac.getMessage();
		String producerName = messagePac.getProducerName();
		return service.receiveMessage(message, producerName);
	}

	/**
	 * 为 消费者 提供消息。<br/>
	 * PS：本来应该是get，但get不适合有请求体
	 *
	 * @return 消息 list
	 */
	@PostMapping("getMessageBatch")
	public BaseResponsePack getMessageBatch(@RequestBody ConsumerSubscribeInfoPack subscribeInfoPack) {
		//提取数据
		Map<String, List<String>> subInfoMap = subscribeInfoPack.getSubInfoMap();
		long pullBatchSize = subscribeInfoPack.getPullBatchSize();
		return service.provideMessage(subInfoMap, pullBatchSize);
	}

	/**
	 * 处理 消费者 消费结果
	 */
	@PostMapping("solveConsumerFeedback")
	public BaseResponsePack solveConsumerFeedback(@RequestBody List<Message> consumeResult) {
		return service.solveConsumerFeedback(consumeResult);
	}

	/**
	 * 创建 topic。<br/>
	 * 如果已存在 topic，则创建失败。
	 */
	@PostMapping("addTopic")
	public BaseResponsePack addTopic(@RequestBody Map<String, Object> map) {
		String topic = (String) map.get("topic");
		int queueNum = Integer.parseInt((String) map.get("queueNum"));
		return service.addTopic(topic, queueNum);
	}

	/**
	 * 设置 topic 的 queue 数量。<br/>
	 * 如果 broker 正在运行 或 topic 不存在，则设置失败。
	 */
	@PutMapping("setQueueNum")
	public BaseResponsePack setQueueNum(@RequestBody Map<String, Object> map) {
		String topic = (String) map.get("topic");
		int queueNum = (Integer) map.get("queueNum");
		return service.setQueueNum(topic, queueNum);
	}

	/**
	 * 获取当前broker的路由信息
	 */

	@GetMapping("getBrokerRouting")
	public BrokerRoutingInfo getBrokerRouting() {
		return service.getBrokerRouting();
	}

	/**
	 * 关闭 broker:停止心跳，并在 NameServer 中注销
	 */
	@GetMapping("shutdown")
	public BaseResponsePack shutdown() {
		return service.shutdown();
	}

	/**
	 * 开始心跳
	 */
	@GetMapping("start")
	public BaseResponsePack start() {
		return service.start();
	}

	/**
	 * 注册 NameServer
	 */
	@PostMapping("registerNameServer")
	public BaseResponsePack registerNameServer(@RequestBody Map<String, String> map) {
		String nameServerUrl = map.get("nameServerUrl");
		return service.registerNameServer(nameServerUrl);
	}
}