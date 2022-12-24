package com.kevin.applestore.controller;

import com.kevin.applestore.service.AppleStoreService;
import com.kevin.kevinmq.common.BaseResponsePack;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 苹果商店：服务器端。<br/>
 * 每隔一秒 从消息队列拉取一定数量消息。
 */
@RestController
@CrossOrigin
@Data
public class AppleStoreController {
	private AppleStoreService service;

	@Autowired
	public AppleStoreController(AppleStoreService service) {
		this.service = service;
	}

	@GetMapping("testStore")
	public BaseResponsePack testStore(String nameIp){
		return BaseResponsePack.simpleSuccess();
	}

	/**
	 * 设置出售速度（消息接收速度）
	 */
	@PutMapping("setSpeed")
	public BaseResponsePack setSpeed(@RequestBody Map<String,Integer> map){
		return service.setSpeed(map.get("speed"));
	}

	/**
	 * 查询已销售数量
	 */
	@GetMapping("getSoldNum")
	public BaseResponsePack getSoldNum(){
		return new BaseResponsePack(0,service.getSoldNum(),"success") ;
	}
}
