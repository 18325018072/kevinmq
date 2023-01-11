package com.kevin.appleshop.controller;

import com.kevin.kevinmq.common.BaseResponsePack;
import com.kevin.kevinmq.common.Message;
import com.kevin.mqclient.producer.Producer;
import com.kevin.mqclient.producer.SendCallback;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 苹果商店：前端服务器
 */
@RestController
@Data
@CrossOrigin
public class WebShopController {
	/**
	 * 消息生产者
	 */
	private Producer producer;
	private final String PacAttrNum = "num";

	@Autowired
	public WebShopController(Producer producer) {
		producer.setProducerName("shopProducer");
		this.producer = producer;
	}

	@GetMapping("testShop")
	public BaseResponsePack registerNameServer(String nameIp) {
		if (nameIp == null) {
			return BaseResponsePack.simpleFail("need nameServerUrl");
		}
		boolean isSuccessConnected = producer.setNameserverAddr(nameIp);
		if (isSuccessConnected) {
			producer.start();
		} else {
			return BaseResponsePack.simpleFail("invalid address");
		}
		return BaseResponsePack.simpleSuccess();
	}

	@PostMapping("syncSoldApple")
	public BaseResponsePack syncSoldApple(@RequestBody Map<String, Integer> map) {
		Message message = new Message("apple", null, "buy:1");
		for (int i = 0; i < map.get(PacAttrNum); i++) {
			BaseResponsePack res = producer.sendSynchronously(message);
			if (res.getStatus() != 0) {
				return BaseResponsePack.simpleFail(res.getInfo());
			}
		}
		return BaseResponsePack.simpleSuccess();
	}

	@PostMapping("asyncSoldApple")
	public BaseResponsePack asyncSoldApple(@RequestBody Map<String, Integer> map) {
		Message message = new Message("apple", null, "buy:1");
		for (int i = 0; i < map.get(PacAttrNum); i++) {
			producer.sendAsync(message, new SendCallback() {
				@Override
				public void onSuccess(BaseResponsePack sendResult) {
					//成功反馈和跳转
				}
				@Override
				public void onFail(BaseResponsePack sendResult) {
					//失败反馈和跳转
				}
			});

		}
		return BaseResponsePack.simpleSuccess();
	}
}
