package com.kevin.mqclient.consumer;


import com.kevin.mqclient.dao.HttpUtil;
import com.kevin.mqclient.entry.BrokerRoutingInfo;
import com.kevin.mqclient.entry.Message;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.util.*;
import java.util.concurrent.*;

/**
 * 消息消费者。<br/>
 * 消费形式：采用 同步 拉 Pull 模式，能订阅多个topic.
 *
 * @author Kevin2
 */
@NoArgsConstructor
@Data
@Component
public class Consumer {
	/**
	 * 订阅条件：{@code map<topic,List<tag>>}
	 */
	private ConcurrentMap<String, List<String>> subscriptionMap = new ConcurrentHashMap<>();
	/**
	 * Consumer名
	 */
	private String consumerName = "Default Consumer";
	/**
	 * 符合订阅要求的{@code  List<broker> }列表
	 */
	private List<BrokerRoutingInfo> brokerRoutingInfoList;
	/**
	 * 运行 & 订阅后，会向 NameServer 发送询问 /30s。
	 */
	private boolean running = false;
	private final String ALL_SUB = "*";
	private boolean localBrokerInfoInitialized = false;
	/**
	 * Timer：向 NameServer 发送询问 /30s。
	 */
	Timer heartToNameserverTimer;

	@Autowired
	private HttpUtil httpUtil;

	public Consumer(String name) {
		this.consumerName = name;
	}

	/**
	 * 添加订阅消息
	 *
	 * @param subExpression 子表达式，用于筛选。或为tag，或为*
	 */
	public void addSubscribe(String topic, String subExpression) {
		if (subscriptionMap.containsKey(topic)) {
			subscriptionMap.get(topic).addAll(solveSubExpression(subExpression));
		} else {
			subscriptionMap.put(topic, solveSubExpression(subExpression));
		}
	}

	/**
	 * 解析 订阅表达式 为 tagList
	 */
	private List<String> solveSubExpression(String subExpression) {
		List<String> tagRes = new ArrayList<>();
		if (subExpression == null || subExpression.equals(ALL_SUB) || subExpression.length() == 0) {
			//这三种情况说明要订阅全部,将subExpression统一设为ALL_SUB
			tagRes.add(ALL_SUB);
		} else {
			//subExpression没那么简单，就需要解析了
			String[] tags = subExpression.split("\\|\\|");
			for (String tag : tags) {
				if (tag.length() > 0) {
					tag = tag.trim();
					if (tag.length() > 0) {
						tagRes.add(tag.trim());
					}
				}
			}
		}
		return tagRes;
	}

	/**
	 * 清空订阅消息
	 */
	public void clearSubscribe() {
		subscriptionMap.clear();
	}

	/**
	 * 接受来自 broker 的消息
	 */
	public List<Message> pull(long pullBatchSize) throws Exception {
		if (!running) {
			throw new Exception("consumer haven't start yet");
		}
		if (brokerRoutingInfoList == null || brokerRoutingInfoList.isEmpty()) {
			return null;
		}
		//随机请求一个broker
		BrokerRoutingInfo targetBrokerRoutingInfo = brokerRoutingInfoList.get(new Random().nextInt(brokerRoutingInfoList.size()));
		List<Message> resMsgList = httpUtil.pullFromBroker(targetBrokerRoutingInfo, subscriptionMap, pullBatchSize);
		//如果不满足需求，请求其他broker
		while (resMsgList.size() < pullBatchSize) {
			for (BrokerRoutingInfo brokerRoutingInfo : brokerRoutingInfoList) {
				List<Message> messageList = httpUtil.pullFromBroker(brokerRoutingInfo, subscriptionMap, pullBatchSize - resMsgList.size());
				resMsgList.addAll(messageList);
			}
		}
		//响应处理成功的 消息
		httpUtil.feedback(resMsgList);
		return resMsgList;
	}

	/**
	 * 向 NameServer 查询符合要求的 Broker 的信息。
	 */
	private void getBrokersFromNameServer() {
		Map<String, List<BrokerRoutingInfo>> res = httpUtil.getBrokersByTopics(subscriptionMap.keySet());
		res.forEach((topic, brokers) -> {
			brokerRoutingInfoList = new ArrayList<>();
			brokerRoutingInfoList.addAll(brokers);
		});
	}

	/**
	 * 启动 consumer，开始请求 NameServer
	 */
	public void start() {
		if (!running) {
			running = true;
			heartToNameserverTimer = new Timer(consumerName + "-heart-ToNameServer");
			heartToNameserverTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					getBrokersFromNameServer();
				}
			}, 0, 30000);
			System.out.println(consumerName + " 启动完毕");
		}
	}

	/**
	 * 停止运行，停止询问的心跳
	 */
	public void shutdown() {
		if (running) {
			heartToNameserverTimer.cancel();
			running = false;
		}
	}
}
