package com.kevin.applestore.service;

import com.kevin.kevinmq.common.BaseResponsePack;
import com.kevin.kevinmq.common.Message;
import com.kevin.mqclient.consumer.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Service
public class AppleStoreService {

	/**
	 * 已售出的数量
	 */
	private long soldNum;

	/**
	 * 每秒能出售的数量（每秒拉取的消息数量）
	 */
	private int sellSpeed = 10;

	/**
	 * 每秒拉取消息
	 */
	private Timer getMessageTimer;

	private final Consumer consumer;

	@Autowired
	public AppleStoreService(Consumer consumer) {
		consumer.setConsumerName("storeConsumer");
		consumer.addSubscribe("apple", null);
		this.consumer = consumer;
	}

	/**
	 * 连接 NameServer，并开始拉取
	 */
	public BaseResponsePack tryConnect(String nameIp) {
		if (nameIp == null) {
			return BaseResponsePack.simpleFail("need nameServerUrl");
		}
		boolean isSuccessConnected = consumer.setNameserverAddr(nameIp);
		if (isSuccessConnected) {
			consumer.start();
			//每秒拉取消息
			getMessageTimer = new Timer("getMessageTimer");
			getMessageTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					if (consumer.isRunning()) {
						List<Message> messageList = consumer.pull(sellSpeed);
						if (messageList != null && !messageList.isEmpty()) {
							System.out.println("store get messageList num: " + messageList.size());
							processMessageList(messageList);
						}
					}
				}
			}, 0, 1000);
		} else {
			return BaseResponsePack.simpleFail("invalid address");
		}
		return BaseResponsePack.simpleSuccess();
	}

	/**
	 * 出售（处理消息）
	 */
	public void processMessageList(List<Message> messageList) {
		for (Message message : messageList) {
			if ("apple".equals(message.getTopic())) {
				String body = message.getBody();
				if (body.contains("buy")) {
					soldNum += Integer.parseInt(body.split(":")[1]);
				}
			}
		}
	}

	/**
	 * 获取已出售的数量
	 */
	public long getSoldNum() {
		return soldNum;
	}

	/**
	 * 设置出售速度（拉取速度）
	 */
	public BaseResponsePack setSpeed(int speed) {
		sellSpeed = speed;
		return BaseResponsePack.simpleSuccess();
	}


}
