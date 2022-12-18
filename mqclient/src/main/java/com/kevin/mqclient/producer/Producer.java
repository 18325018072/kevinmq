package com.kevin.mqclient.producer;

import com.kevin.mqclient.dao.HttpUtil;
import com.kevin.mqclient.dao.Logger;
import com.kevin.mqclient.entry.BrokerRoutingInfo;
import com.kevin.mqclient.entry.Log;
import com.kevin.mqclient.entry.Message;
import com.kevin.mqclient.entry.pac.BaseResponsePack;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@AllArgsConstructor
@Data
@Component
public class Producer {
	/**
	 * {@code Brokers 信息：Map<topic,List<Broker>>}
	 */
	private Map<String, List<BrokerRoutingInfo>> topicBrokerMap = new HashMap<>();
	private String producerName = "Default Producer";
	private Logger logger;
	private ThreadPoolExecutor threadPool=new ThreadPoolExecutor(10, 10, 5, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadFactory() {
		int i=0;
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r,producerName+"-sendMessagePool-"+ i++);
		}
	});

	@Autowired
	private HttpUtil httpUtil;

	public Producer(String producerName) {
		this.producerName = producerName;
	}

	/**
	 * 向 NameServer 查询 topicSet 的 Broker 的信息 /30s。
	 */
	private void getBrokersFromNameServer(Set<String> topicSet) {
		topicBrokerMap = httpUtil.getBrokersByTopics(topicSet);
	}

	/**
	 * 向 NameServer 查询某 topic 的 Broker 的信息 /30s。
	 */
	private boolean getBrokersFromNameServer(String hopeTopic) {
		Map<String, List<BrokerRoutingInfo>> brokerMap = httpUtil.getBrokersByTopics(Collections.singleton(hopeTopic));
		if (brokerMap!=null) {
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
		//检查topic-broker路由
		if (!topicBrokerMap.containsKey(msg.getTopic())) {
			if (!getBrokersFromNameServer(msg.getTopic())){
				return new BaseResponsePack(1,msg,"Broker_NotFound");
			};
		}
		//选择目标 queue
		List<BrokerRoutingInfo> brokerRoutingInfoList = topicBrokerMap.get(msg.getTopic());
		BrokerRoutingInfo targetBrokerRoutingInfo = brokerRoutingInfoList.get(new Random().nextInt(brokerRoutingInfoList.size()));
		List<Integer> queueIdList = targetBrokerRoutingInfo.getTopicInfo().get(msg.getTopic());
		Integer targetQueueId = queueIdList.get(new Random().nextInt(queueIdList.size()));
		msg.setQueueId(targetQueueId);
		//发送
		BaseResponsePack sendResult = httpUtil.sendSynchronously(msg, targetBrokerRoutingInfo,producerName);
		//日志记录
		logger.saveLog(new Log(new Date(),"send message","success"));
		return sendResult;
	}

	/**
	 * 发送异步消息
	 *
	 * @param callback 回调，处理 SendResult 对象
	 */
	public void sendAsync(Message msg, SendCallback callback) {
		threadPool.execute(() -> {
			//发送
			BaseResponsePack sendResult = sendSynchronously(msg);
			if (sendResult.getStatus()==0) {
				callback.onSuccess(sendResult);
			}else {
				callback.onFail(sendResult);
			}
		});
	}
}