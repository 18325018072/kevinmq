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
	private final Timer getMessageTimer;

	private final Consumer consumer = new Consumer("appleConsumer");

	public AppleStoreService() {
		consumer.addSubscribe("apple", null);
		getMessageTimer = new Timer("getMessageTimer");
		getMessageTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (consumer.isRunning()) {
					try {
						List<Message> messageList = consumer.pull(sellSpeed);
						if (messageList != null && !messageList.isEmpty()) {
							processMessageList(messageList);
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		}, 0, 1000);
	}

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

	public long getSoldNum() {
		return soldNum;
	}

	public BaseResponsePack setSpeed(int speed) {
		sellSpeed = speed;
		return BaseResponsePack.simpleSuccess();
	}
}
