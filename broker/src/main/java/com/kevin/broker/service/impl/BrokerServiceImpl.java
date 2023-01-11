package com.kevin.broker.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kevin.broker.dao.HttpUtil;
import com.kevin.broker.dao.mapper.LogMapper;
import com.kevin.broker.dao.mapper.MessageMapper;
import com.kevin.broker.service.MessageService;
import com.kevin.kevinmq.common.BrokerRoutingInfo;
import com.kevin.broker.entry.MessageQueue;
import com.kevin.broker.service.BrokerService;
import com.kevin.kevinmq.common.BaseResponsePack;
import com.kevin.kevinmq.common.Log;
import com.kevin.kevinmq.common.Message;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 20349
 * @description 针对表【message(消息 存储表，用于持久化，防止宕机)】的数据库操作Service实现
 * @createDate 2022-12-19 19:56:44
 */
@Service
public class BrokerServiceImpl implements BrokerService {
	MessageService messageService;

	/**
	 * 消息的存储主体
	 */
	private final List<MessageQueue> commitLog = new LinkedList<>();

	/**
	 * 索引：作为消费消息的索引，保存了指定 Topic 的队列消息在 CommitLog 中的位置。
	 * {@code ConsumeQueue = HashMap<topic,List<Queue> >}
	 */
	private final Map<String, List<MessageQueue>> consumeQueue = new HashMap<>();

	/**
	 * start 该broker时自动设置，为 brokername.hashcode
	 */
	private Integer brokerId;

	/**
	 * broker名：用于人理解
	 */
	@Value("${spring.application.name}")
	private String brokerName;

	@Value("${server.port}")
	private int brokerPort;

	private final String ALL_SUB_TAG = "*";
	private boolean running;
	/**
	 * 每隔30s 发送心跳到 NameServer：注册 broker 信息
	 */
	private Timer timerToNameServer;

	private final HttpUtil httpUtil;

	private final LogMapper logMapper;

	@Autowired
	public BrokerServiceImpl(HttpUtil httpUtil, LogMapper logMapper, MessageService messageService) {
		this.messageService = messageService;
		this.httpUtil = httpUtil;
		this.logMapper = logMapper;
	}

	@Override
	public String getBrokerStatus() {
		return (running ? "running" : "shutdown");
	}

	/**
	 * 接收 生产者的消息。如果没topic，拒绝；如果没tag，设为*。
	 */
	@Override
	public BaseResponsePack receiveMessage(Message message, String producerName) {
		//1.检查topic
		String msgTopic = message.getTopic();
		if (!consumeQueue.containsKey(msgTopic)) {
			return new BaseResponsePack(1, null, "Broker Don't have such topic");
		}
		//2.找到对应queue
		Integer msgQueueId = message.getQueueId();
		MessageQueue messageQueue;
		try {
			messageQueue = consumeQueue.get(msgTopic).get(msgQueueId);
		} catch (IndexOutOfBoundsException e) {
			return new BaseResponsePack(1, null, "Broker's topic Don't have such queueId");
		}
		//3.处理tag
		if (message.getTag() == null || "".equals(message.getTag())) {
			message.setTag(ALL_SUB_TAG);
		}
		//设置消费状态
		message.setConsumeStatus(new AtomicInteger(0));
		//持久化：存入数据库。会自动添加ID
		messageService.save(message);
		//存入缓存
		messageQueue.addMessage(message);
		logMapper.insert(new Log("broker:receiveMessage", message));
		return new BaseResponsePack(0, message.getMessageId(), "success");
	}

	/**
	 * 为 消费者 提供 消息list。<br/>如果没tag，跳过不给（这意味着当消费者通吃时，消费者必须显示声明订阅条件为*）。
	 */
	@Override
	public BaseResponsePack getMessageBatch(Map<String, List<String>> subInfoMap, long pullBatchSize) {
		if (subInfoMap == null || subInfoMap.isEmpty()) {
			return BaseResponsePack.simpleFail("empty subscribe");
		} else if (pullBatchSize == 0) {
			return BaseResponsePack.simpleFail("pullBatchSize can't be 0");
		}
		List<Message> res = new ArrayList<>();
		Set<Map.Entry<String, List<String>>> subEntries = subInfoMap.entrySet();
		for (Map.Entry<String, List<String>> subEntry : subEntries) {
			//查询本broker是否有订阅的topic
			List<MessageQueue> messageQueueList = consumeQueue.get(subEntry.getKey());
			if (messageQueueList == null) {
				continue;
			}
			//提取tag
			List<String> subTagList = subEntry.getValue();
			if (subTagList == null || subTagList.isEmpty()) {
				continue;
			}
			//进入messageQueueList中提取 未消费的消息
			for (MessageQueue messageQueue : messageQueueList) {
				List<Message> messageList = messageQueue.getData();
				for (Message message : messageList) {
					//提取 tag 符合的消息
					boolean isMessageAbleSend = subTagList.contains(ALL_SUB_TAG) || ALL_SUB_TAG.equals(message.getTag()) || subTagList.contains(message.getTag());
					if (isMessageAbleSend && message.getConsumeStatus().compareAndSet(0, 1)) {
						res.add(message);
					}
					//如果数量够了，则可以返回
					if (res.size() >= pullBatchSize) {
						logMapper.insert(new Log("broker:getMessageBatch", "provide num:" + res.size()));
						return new BaseResponsePack(0, res, "success:enough");
					}
				}
			}
		}
		//数量不够，但扫描完毕了，只能返回
		logMapper.insert(new Log("broker:getMessageBatch", "provide num:" + res.size()));
		return new BaseResponsePack(0, res, "success:not enough");
	}

	/**
	 * 处理 消费者 消费结果
	 */
	@Override
	public BaseResponsePack solveConsumerFeedback(@RequestBody List<Message> consumeResult) {
		System.out.println("solve feedback num:" + consumeResult.size());
		for (Message message : consumeResult) {
			MessageQueue messageQueue = consumeQueue.get(message.getTopic()).get(message.getQueueId());
			//从java缓冲中删除
			messageQueue.removeMessage(message);
			//从数据库删除
			messageService.removeById(message.getMessageId());
			logMapper.insert(new Log("broker:consume success", message));
			//TODO:没消费成功的，还原消息的消费状态
		}
		return BaseResponsePack.simpleSuccess();
	}

	/**
	 * 创建 topic。<br/>
	 * 如果已存在 topic，则创建失败。
	 */
	@Override
	public BaseResponsePack addTopic(String topic, int queueNum) {
		if (consumeQueue.containsKey(topic)) {
			return BaseResponsePack.simpleFail("The topic already exists");
		}
		List<MessageQueue> queueList = new ArrayList<>();
		for (int i = 0; i < queueNum; i++) {
			MessageQueue queue = new MessageQueue(brokerId, topic, i, new CopyOnWriteArrayList<>());
			queueList.add(queue);
			commitLog.add(queue);
		}
		consumeQueue.put(topic, queueList);
		logMapper.insert(new Log("broker:addTopic", topic + "(" + queueNum + ")"));
		return BaseResponsePack.simpleSuccess();
	}

	/**
	 * 设置 topic 的 queue 数量。<br/>
	 * 如果 broker 正在运行 或 topic 不存在，则设置失败。
	 */
	@Override
	public BaseResponsePack setQueueNum(String topic, int queueNum) {
		List<MessageQueue> queueList = consumeQueue.get(topic);
		if (queueList == null) {
			System.out.println("setQueueNum失败，topic不存在");
			return BaseResponsePack.simpleFail("setQueueNum失败，topic不存在");
		} else if (running) {
			return BaseResponsePack.simpleFail("setQueueNum失败，broker正在运行");
		}
		while (queueList.size() > queueNum) {
			commitLog.remove(queueList.get(0));
			queueList.remove(0);
		}
		while (queueList.size() < queueNum) {
			MessageQueue queue = new MessageQueue(brokerId, topic, queueList.size(), new CopyOnWriteArrayList<>());
			queueList.add(queue);
			commitLog.add(queue);
		}
		logMapper.insert(new Log("broker:setQueueNum", topic + "(" + queueNum + ")"));
		return BaseResponsePack.simpleSuccess();
	}

	/**
	 * 获取当前broker的路由信息
	 */
	@SneakyThrows
	@Override
	public BrokerRoutingInfo getBrokerRouting() {
		Map<String, List<Integer>> topicInfo = new HashMap<>(5);
		consumeQueue.forEach((topic, queueList) -> {
			List<Integer> queueIdList = new ArrayList<>();
			queueList.forEach((queue) -> {
				queueIdList.add(queue.getQueueId());
			});
			topicInfo.put(topic, queueIdList);
		});
		return new BrokerRoutingInfo(InetAddress.getLocalHost().getHostAddress(), brokerPort, brokerName, topicInfo);
	}

	/**
	 * 关闭 broker:停止心跳，并在 NameServer 中注销
	 */
	@SneakyThrows
	@Override
	public BaseResponsePack shutdown() {
		running = false;
		timerToNameServer.cancel();
		//通知 NameServer
		BrokerRoutingInfo brokerRoutingInfo = new BrokerRoutingInfo(InetAddress.getLocalHost().getHostAddress(), brokerPort, brokerName, null);
		httpUtil.sendShutDownToNameServer(brokerRoutingInfo);
		logMapper.insert(new Log("broker:shutdown", brokerName));
		return BaseResponsePack.simpleSuccess();
	}

	/**
	 * 开始心跳
	 */
	@Override
	public BaseResponsePack start() {
		brokerId = brokerName.hashCode();
		running = true;
		//每隔30s 发送心跳到 NameServer：注册 broker 信息
		timerToNameServer = new Timer("timerToNameServer");
		timerToNameServer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (running) {
					httpUtil.sendInfoToNameServer(getBrokerRouting());
				}
			}
		}, 0, 30000);
		logMapper.insert(new Log("broker:start", brokerName));
		return BaseResponsePack.simpleSuccess();
	}

	/**
	 * 注册 NameServer
	 */
	@Override
	public BaseResponsePack registerNameServer(String nameServerUrl) {
		if (httpUtil.testUrl(nameServerUrl)) {
			httpUtil.setNameServerUrl(nameServerUrl);
			logMapper.insert(new Log("broker:register NameServer", brokerName + "->" + nameServerUrl));
			return BaseResponsePack.simpleSuccess();
		} else {
			return BaseResponsePack.simpleFail("url can't connect");
		}
	}
}




