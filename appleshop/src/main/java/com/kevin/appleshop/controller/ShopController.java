package com.kevin.appleshop.controller;

import com.kevin.kevinmq.common.BaseResponsePack;
import com.kevin.mqclient.producer.Producer;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Data
@NoArgsConstructor
public class ShopController {
	public Producer producer=new Producer("shopProducer");
	@PostMapping("syncSold")
	public BaseResponsePack syncSold(int num){
//		Message message=new Message("apple","buy one",)
//		producer.sendSynchronously()
		return null;
	}
}
