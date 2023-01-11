package com.kevin.mqclient.consumer;


import com.kevin.kevinmq.common.BrokerRoutingInfo;
import com.kevin.kevinmq.common.Message;
import com.kevin.mqclient.dao.HttpUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
	private List<BrokerRoutingInfo> brokerRoutingInfoList = new ArrayList<>();
	;

	/**
	 * 运行 & 订阅后，会向 NameServer 发送询问 /30s。
	 */
	private boolean running = false;
	private static final String ALL_SUB = "*";
	private static final int MAX_TRY_GET_TIME = 5;
	private boolean localBrokerInfoInitialized = false;
	/**
	 * Timer：向 NameServer 发送询问 /30s。
	 */
	Timer heartToNameserverTimer;

	private HttpUtil httpUtil;

	/**
	 * 注册到的名称服务器
	 */
	private String nameServerUrl;

	@Autowired
	public Consumer(HttpUtil httpUtil) {
		this.httpUtil = httpUtil;
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
		List<String> tagList = new ArrayList<>();
		if (subExpression == null || subExpression.equals(ALL_SUB) || subExpression.length() == 0) {
			//这三种情况说明要订阅全部,将subExpression统一设为ALL_SUB
			tagList.add(ALL_SUB);
		} else {
			//subExpression没那么简单，就需要解析了
			String[] tags = subExpression.split("\\|\\|");
			for (String tag : tags) {
				if (tag.length() > 0) {
					tag = tag.trim();
					if (tag.length() > 0) {
						tagList.add(tag.trim());
					}
				}
			}
		}
		return tagList;
	}

	/**
	 * 删除订阅消息
	 */
	public void removeSubscribe(String topic) {
		subscriptionMap.remove(topic);
	}

	/**
	 * 向 broker 拉取一定数量的消息
	 */
	public List<Message> pull(long pullBatchSize) {
		if (!running) {
			throw new RuntimeException("consumer haven't start yet");
		}
		if (brokerRoutingInfoList == null || brokerRoutingInfoList.isEmpty()) {
			return null;
		}
		//1.随机请求一个broker
		BrokerRoutingInfo targetBrokerRoutingInfo = brokerRoutingInfoList.get(new Random().nextInt(brokerRoutingInfoList.size()));
		List<Message> resMsgList = new ArrayList<>();
		List<Message> getList = httpUtil.pullFromBroker(targetBrokerRoutingInfo, subscriptionMap, pullBatchSize);
		if (getList != null) {
			resMsgList.addAll(getList);
			//响应处理成功的 消息
			httpUtil.feedback(getList, targetBrokerRoutingInfo);
		}
		//2.如果不满足需求，请求其他broker,最多循环MAX_TRY_GET_TIME次
		for (int tryTime = 0; tryTime < MAX_TRY_GET_TIME && resMsgList.size() < pullBatchSize; tryTime++) {
			for (BrokerRoutingInfo brokerRoutingInfo : brokerRoutingInfoList) {
				getList = httpUtil.pullFromBroker(brokerRoutingInfo, subscriptionMap, pullBatchSize - resMsgList.size());
				if (getList != null) {
					resMsgList.addAll(getList);
					//响应处理成功的 消息
					httpUtil.feedback(getList, targetBrokerRoutingInfo);
				}
			}
		}
		return resMsgList;
	}

	/**
	 * 向 NameServer 查询符合要求的 Broker 的信息。
	 */
	private void getBrokersFromNameServer() {
		if (subscriptionMap.isEmpty()) {
			return;
		}
		Map<String, List<BrokerRoutingInfo>> res = httpUtil.getBrokersByTopics(subscriptionMap.keySet(), nameServerUrl);
		brokerRoutingInfoList.clear();
		res.forEach((topic, brokerList) -> brokerRoutingInfoList.addAll(brokerList));
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
