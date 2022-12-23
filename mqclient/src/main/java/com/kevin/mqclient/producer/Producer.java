package com.kevin.mqclient.producer;

import com.kevin.kevinmq.common.BaseResponsePack;
import com.kevin.kevinmq.common.BrokerRoutingInfo;
import com.kevin.kevinmq.common.Message;
import com.kevin.mqclient.dao.HttpUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 消息发送者
 * 没有绑定关系，每次发送前都要查询。
 * Producer 基本无状态
 *
 * @author Kevin2
 */
@NoArgsConstructor
@Data
@Slf4j
@Component
public class Producer {
	/**
	 * {@code Brokers 信息：Map<topic,List<Broker>>}
	 */
	private Map<String, List<BrokerRoutingInfo>> topicBrokerMap = new HashMap<>();

	private String producerName = "Default Producer";

	private ThreadPoolExecutor asyncSendPool = new ThreadPoolExecutor(10, 100, 5, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadFactory() {
		int i = 0;

		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, producerName + "-sendMessagePool-" + i++);
		}
	});

	@Autowired
	private HttpUtil httpUtil;

	private boolean running = false;

	/**
	 * 每隔30s 拉取 Topic 路由信息
	 */
	private Timer updateBrokerInfoTimer;

	/**
	 * 注册到的名称服务器
	 */
	private String nameServerUrl;

	public Producer(String producerName) {
		this.producerName = producerName;
	}

	/**
	 * 设置命名服务器
	 */
	public boolean setNameserverAddr(String url) {
		if (httpUtil.testUrl(url)) {
			nameServerUrl = url;
			return true;
		}
		return false;
	}

	/**
	 * 启动 Producer
	 */
	public void start() {
		if (running){
			return;
		}
		if (nameServerUrl == null) {
			throw new RuntimeException("please set nameserver first");
		}
		updateBrokerInfoTimer=new Timer("updateBrokerInfoTimer");
		updateBrokerInfoTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				searchBrokersFromNameServer(topicBrokerMap.keySet());
			}
		},0,30000);
	}

	/**
	 * 关闭Producer
	 */
	public void shutdown(){
		if (!running){
			return;
		}
		updateBrokerInfoTimer.cancel();
	}

	/**
	 * 向 NameServer 查询 topicSet 的 Broker 的信息 /30s。
	 */
	private void searchBrokersFromNameServer(Set<String> topicSet) {
		if (nameServerUrl==null){
			throw new RuntimeException("please set nameserver first");
		}
		topicBrokerMap = httpUtil.getBrokersByTopics(topicSet, nameServerUrl);
	}

	/**
	 * 向 NameServer 查询某 topic 的 Broker 的信息 /30s。
	 */
	private boolean searchBrokersFromNameServer(String hopeTopic) {
		if (nameServerUrl==null){
			throw new RuntimeException("please set nameserver first");
		}
		Map<String, List<BrokerRoutingInfo>> brokerMap = httpUtil.getBrokersByTopics(Collections.singleton(hopeTopic), nameServerUrl);
		if (brokerMap != null) {
			topicBrokerMap.put(hopeTopic, brokerMap.get(hopeTopic));
			return true;
		}
		return false;
	}

	/**
	 * 发送同步消息
	 *
	 * @return 发送结果
	 */
	public BaseResponsePack sendSynchronously(Message msg) {
		if (!running){
			throw new RuntimeException("please start producer first");
		}
		//检查topic-broker路由
		if (!topicBrokerMap.containsKey(msg.getTopic())) {
			if (!searchBrokersFromNameServer(msg.getTopic())) {
				return new BaseResponsePack(1, msg, "Broker_NotFound");
			}
		}
		//随机选择一个 broker
		List<BrokerRoutingInfo> brokerRoutingInfoList = topicBrokerMap.get(msg.getTopic());
		BrokerRoutingInfo targetBrokerRoutingInfo = brokerRoutingInfoList.get(new Random().nextInt(brokerRoutingInfoList.size()));
		//随机选择一个 queue
		List<Integer> queueIdList = targetBrokerRoutingInfo.getTopicInfo().get(msg.getTopic());
		Integer targetQueueId = queueIdList.get(new Random().nextInt(queueIdList.size()));
		msg.setQueueId(targetQueueId);
		//发送
		return httpUtil.sendSynchronously(msg, targetBrokerRoutingInfo, producerName,nameServerUrl);
	}

	/**
	 * 发送异步消息
	 *
	 * @param callback 回调，处理 SendResult 对象
	 */
	public void sendAsync(Message msg, SendCallback callback) {
		if (!running){
			throw new RuntimeException("please start producer first");
		}
		asyncSendPool.execute(() -> {
			//发送
			BaseResponsePack sendResult = sendSynchronously(msg);
			if (sendResult.getStatus() == 0) {
				callback.onSuccess(sendResult);
			} else {
				callback.onFail(sendResult);
			}
		});
	}
}