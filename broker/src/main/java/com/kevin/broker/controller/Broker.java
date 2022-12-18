package com.kevin.broker.controller;


import com.kevin.broker.dao.HttpUtil;
import com.kevin.broker.entry.BrokerRoutingInfo;
import com.kevin.broker.entry.Message;
import com.kevin.broker.entry.MessageQueue;
import com.kevin.broker.entry.pac.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.util.*;

/**
 * 服务器<br/>
 *
 * @author Kevin2
 */
@Data
@NoArgsConstructor
@RestController
@CrossOrigin
public class Broker {
	/**
	 * 消息的存储主体
	 */
	private List<MessageQueue> commitLog = new LinkedList<>();

	/**
	 * 索引：作为消费消息的索引，保存了指定 Topic 的队列消息在 CommitLog 中的位置。
	 * {@code ConsumeQueue = HashMap<topic,List<Queue> >}
	 */
	private Map<String, List<MessageQueue>> consumeQueue = new HashMap<>();
	private Integer brokerId;

	/**
	 * broker名：用于人理解
	 */
	@Value("${spring.application.name}")
	private String brokerName;

	@Value("${server.port}")
	private int brokerPort;

	private final String ALL_SUB_TAG = "*";
	private boolean running = false;
	/**
	 * 每隔30s 发送心跳到 NameServer：注册 broker 信息
	 */
	private Timer timerToNameServer;

	@Autowired
	private HttpUtil httpUtil;

	@GetMapping("testBroker")
	public  BaseResponsePack testUrl(){
		return new BaseResponsePack(BaseResponsePack.SUCCESS_CODE,null,(running?"running":"shutdown"));
	}

	/**
	 * 接收 生产者的消息
	 */
	@PostMapping("postMessage")
	public SendResult receiveMessage(@RequestBody MessagePackFromProducer messagePac) {
		//0.提取数据
		Message message = messagePac.getMessage();
		String producerName = messagePac.getProducerName();
		//1.检查topic
		String msgTopic = message.getTopic();
		if (!consumeQueue.containsKey(msgTopic)) {
			return new SendResult(SendStatus.Send_Error, message.getMessageId(), "Broker Don't have such topic");
		}
		//2.找到对应queue
		Integer msgQueueId = message.getQueueId();
		MessageQueue messageQueue;
		try {
			messageQueue = consumeQueue.get(msgTopic).get(msgQueueId);
		} catch (IndexOutOfBoundsException e) {
			return new SendResult(SendStatus.Send_Error, message.getMessageId(), "Broker's topic Don't have such queueId");
		}
		//3.添加消息
		if (message.getTags() == null || "".equals(message.getTags())) {
			message.setTags(ALL_SUB_TAG);
		}
		messageQueue.addMessage(message);
		return new SendResult(SendStatus.Send_OK, message.getMessageId(), "success");
	}

	/**
	 * 为 消费者 提供消息。<br/>
	 * PS：本来应该是get，但get不适合有请求体
	 *
	 * @return 消息 list
	 */
	@PostMapping("getMessageBatch")
	public List<Message> getMessageBatch(@RequestBody ConsumerSubscribeInfoPack subscribeInfoPack) {
		//提取数据
		Map<String, String> subInfoMap = subscribeInfoPack.getSubInfoMap();
		long pullBatchSize = subscribeInfoPack.getPullBatchSize();

		List<Message> res = new ArrayList<>();
		Set<Map.Entry<String, String>> subEntries = subInfoMap.entrySet();
		for (Map.Entry<String, String> subEntry : subEntries) {
			//查询本broker是否有订阅的topic
			List<MessageQueue> messageQueueList = consumeQueue.get(subEntry.getKey());
			if (messageQueueList == null) {
				continue;
			}
			//提取tag
			String subTag = subEntry.getValue();
			if (subTag == null || "".equals(subTag)) {
				continue;
			}
			String[] split = subTag.split("\\|\\|");
			String[] subTags = new String[split.length];
			for (int i = 0; i < split.length; i++) {
				subTags[i] = split[i].trim();
			}
			//进入messageQueueList中提取 未消费的消息
			for (MessageQueue messageQueue : messageQueueList) {
				List<Message> messageList = messageQueue.getData();
				for (Message message : messageList) {
					if (!message.getConsumeStatus().compareAndSet(0, 1)) {
						continue;
					}
					//检查 messageQueueList 中每条消息的tag是否和订阅要求符合
					if (subTag.equals(ALL_SUB_TAG)) {
						res.add(message);
					} else {
						//subTags 是否符合 该消息tag
						for (String tag : subTags) {
							if (tag.equals(message.getTags())) {
								res.add(message);
								break;
							}
						}
					}
					//如果数量够了，则可以返回
					if (res.size() >= pullBatchSize) {
						return res;
					}
				}
			}
		}
		return res;
	}

	/**
	 * 处理 消费者 消费结果
	 */
	@PostMapping("solveConsumerFeedback")
	public BaseResponsePack solveConsumerFeedback(@RequestBody List<Message> consumeResult) {
		for (Message message : consumeResult) {
			MessageQueue messageQueue = consumeQueue.get(message.getTopic()).get(message.getQueueId());
			try {
				if (message.getConsumeStatus().get() == 2) {
					//从java缓冲中删除
					messageQueue.removeMessage(message);
					//TODO：从数据库删除
				} else {
					messageQueue.initMessage(message);
				}
			} catch (NullPointerException e) {
				return BaseResponsePack.simpleFail("messages not exist");
			}
		}
		return BaseResponsePack.simpleSuccess();
	}

	/**
	 * 创建 topic。<br/>
	 * 如果已存在 topic，则创建失败。
	 */
	@PostMapping("addTopic")
	public BaseResponsePack addTopic(@RequestBody Map<String, Object> map) {
		String topic = (String) map.get("topic");
		int queueNum = Integer.parseInt((String) map.get("queueNum")) ;
		if (consumeQueue.containsKey(topic)) {
			return BaseResponsePack.simpleFail("topic has been set");
		}
		List<MessageQueue> queueList = new ArrayList<>();
		for (int i = 0; i < queueNum; i++) {
			MessageQueue queue = new MessageQueue(brokerId, topic, i, new ArrayList<>());
			queueList.add(queue);
			commitLog.add(queue);
		}
		consumeQueue.put(topic, queueList);
		return BaseResponsePack.simpleSuccess();
	}

	/**
	 * 设置 topic 的 queue 数量。<br/>
	 * 如果 broker 正在运行 或 topic 不存在，则设置失败。
	 */
	@PutMapping("setQueueNum")
	public BaseResponsePack setQueueNum(@RequestBody Map<String, Object> map) {
		String topic = (String) map.get("topic");
		int queueNum = (Integer) map.get("queueNum");
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
			MessageQueue queue = new MessageQueue(brokerId, topic, queueList.size(), new ArrayList<>());
			queueList.add(queue);
			commitLog.add(queue);
		}
		return BaseResponsePack.simpleSuccess();
	}

	/**
	 * 获取当前broker的路由信息
	 */
	@SneakyThrows
	@GetMapping("getBrokerRouting")
	public BrokerRoutingInfo getBrokerRouting() {
		Map<String, List<Integer>> topicInfo = new HashMap<>();
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
	@GetMapping("shutdown")
	public BaseResponsePack shutdown() {
		running = false;
		try {
			timerToNameServer.cancel();
			//通知 NameServer
			BrokerRoutingInfo brokerRoutingInfo = new BrokerRoutingInfo(InetAddress.getLocalHost().getHostAddress(), brokerPort, brokerName, null);
			httpUtil.sendShutDownToNameServer(brokerRoutingInfo);
		} catch (Exception e) {
			e.printStackTrace();
			return BaseResponsePack.simpleFail("shutdown fail:" + e.getMessage());
		}
		return BaseResponsePack.simpleSuccess();
	}

	/**
	 * 开始心跳
	 */
	@GetMapping("start")
	public BaseResponsePack start() {
		brokerId = brokerName.hashCode();
		running = true;
		//每隔30s 发送心跳到 NameServer：注册 broker 信息
		timerToNameServer=new Timer("timerToNameServer");
		timerToNameServer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (running) {
					httpUtil.sendInfoToNameServer(getBrokerRouting());
				}
			}
		}, 0, 30000);
		return BaseResponsePack.simpleSuccess();
	}

	/**
	 * 注册 NameServer
	 */
	@PostMapping("registerNameServer")
	public BaseResponsePack registerNameServer(@RequestBody Map<String, String> map) {
		String nameServerUrl = map.get("nameServerUrl");
		if (httpUtil.testUrl(nameServerUrl)) {
			httpUtil.setNameServerUrl(nameServerUrl);
			return BaseResponsePack.simpleSuccess();
		} else {
			return BaseResponsePack.simpleFail("url can't connect");
		}
	}
}