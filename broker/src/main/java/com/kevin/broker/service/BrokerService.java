package com.kevin.broker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kevin.kevinmq.common.BrokerRoutingInfo;
import com.kevin.kevinmq.common.BaseResponsePack;
import com.kevin.kevinmq.common.Message;

import java.util.List;
import java.util.Map;

/**
* @author 20349
* @description 针对表【message(消息 存储表，用于持久化，防止宕机)】的数据库操作Service
* @createDate 2022-12-19 19:56:44
*/
public interface BrokerService {

	String getBrokerStatus();

	/**
	 * 接收 生产者的消息
	 */
	BaseResponsePack receiveMessage(Message message, String producerName);

	/**
	 * 为 消费者 提供消息。<br/>
	 * PS：本来应该是get，但get不适合有请求体
	 *
	 * @return 消息 list
	 */
	BaseResponsePack getMessageBatch(Map<String, List<String>> subInfoMap, long pullBatchSize);

	/**
	 * 处理 消费者 消费结果
	 */
	BaseResponsePack solveConsumerFeedback(List<Message> consumeResult);

	/**
	 * 创建 topic。<br/>
	 * 如果已存在 topic，则创建失败。
	 */
	BaseResponsePack addTopic(String topic, int queueNum);

	/**
	 * 设置 topic 的 queue 数量。<br/>
	 * 如果 broker 正在运行 或 topic 不存在，则设置失败。
	 */
	BaseResponsePack setQueueNum(String topic, int queueNum);

	/**
	 * 获取当前broker的路由信息
	 */
	BrokerRoutingInfo getBrokerRouting();

	/**
	 * 关闭 broker:停止心跳，并在 NameServer 中注销
	 */
	BaseResponsePack shutdown();

	/**
	 * 开始心跳
	 */
	BaseResponsePack start();

	/**
	 * 注册 NameServer
	 */
	BaseResponsePack registerNameServer(String nameServerUrl);
}
