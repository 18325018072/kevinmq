package com.kevin.nameserver.controller;

import com.kevin.nameserver.entry.BrokerRoutingInfo;
import com.kevin.nameserver.entry.pac.BaseResponsePack;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.BiFunction;

/**
 * 命名服务器
 *
 * @author Kevin2
 */
@RestController
@CrossOrigin
public class NameServer {
	/**
	 * 每个 topic 和包括该topic的注册的 Broker 信息：<br/>
	 * {@code HashMap<topic,List<Broker>>}
	 */
	private final Map<String, List<BrokerRoutingInfo>> topicBrokerMap = new HashMap<>();
	/**
	 * 每个 broker 的存活倒计时。server每 10 s扫描一次，12次（2分钟）没响应则移除
	 */
	private final Map<BrokerRoutingInfo, Integer> brokerHp = new HashMap<>();
	private Timer hpTimer;
	private boolean running;

	@GetMapping("testNameServer")
	public  BaseResponsePack testUrl(){
		return new BaseResponsePack(BaseResponsePack.SUCCESS_CODE,null,(running?"running":"shutdown"));
	}

	/**
	 * 接受Broker的心跳： 注册 Broker 的路由信息.
	 */
	@PostMapping("updateBroker")
	public BaseResponsePack updateBroker(@RequestBody BrokerRoutingInfo brokerInfo) {
		//如果已存在，清除原有旧信息
		if (brokerHp.containsKey(brokerInfo)) {
			Set<Map.Entry<String, List<BrokerRoutingInfo>>> entries = topicBrokerMap.entrySet();
			for (Map.Entry<String, List<BrokerRoutingInfo>> entry : entries) {
				entry.getValue().remove(brokerInfo);
			}
		}
		//插入新信息
		Map<String, List<Integer>> topicInfo = brokerInfo.getTopicInfo();
		for (String topic : topicInfo.keySet()) {
			topicBrokerMap.putIfAbsent(topic, new ArrayList<>());
			topicBrokerMap.get(topic).add(brokerInfo);
		}
		//更新存活时间
		brokerHp.put(brokerInfo, 12);
		return BaseResponsePack.simpleSuccess();
	}

	/**
	 * 撤掉一个 broker
	 */
	@DeleteMapping("shutdownBroker")
	public BaseResponsePack deleteBroker(@RequestBody BrokerRoutingInfo brokerInfo){
		//如果已存在，清除原有旧信息
		if (brokerHp.containsKey(brokerInfo)) {
			Set<Map.Entry<String, List<BrokerRoutingInfo>>> entries = topicBrokerMap.entrySet();
			for (Map.Entry<String, List<BrokerRoutingInfo>> entry : entries) {
				entry.getValue().remove(brokerInfo);
			}
			brokerHp.remove(brokerInfo);
			return BaseResponsePack.simpleSuccess();
		}else {
			return BaseResponsePack.simpleFail("broker don't exist");
		}
	}

	/**
	 * 查询所有broker信息
	 */
	@GetMapping("getAllBrokers")
	public BaseResponsePack getAllBrokers(){
		return new BaseResponsePack(BaseResponsePack.SUCCESS_CODE,new ArrayList<>(brokerHp.keySet()) ,"success");
	}

	/**
	 * 通过 topic 查找 broker
	 */
	@GetMapping("getBrokerInfoByTopic")
	public BaseResponsePack getBrokerInfoByTopic(@RequestParam("topicSet") List<String> topicSet) {
		Map<String, List<BrokerRoutingInfo>> res = new HashMap<>();
		for (String hopeTopic : topicSet) {
			res.put(hopeTopic,topicBrokerMap.get(hopeTopic));
		}
		return new BaseResponsePack(BaseResponsePack.SUCCESS_CODE,res,"success");
	}

	/**
	 * 开启 NameServer，开始检测broker心跳
	 */
	@GetMapping("start")
	public BaseResponsePack start() {
		if (running){
			return BaseResponsePack.simpleSuccess();
		}
		running = true;
		//Name Server 每隔 10s 扫描所有存活 broker。2分无消息，则除该 Broker。
		hpTimer=new Timer("hpTimer");
		hpTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				//生命减一
				brokerHp.replaceAll(new BiFunction<BrokerRoutingInfo, Integer, Integer>() {
					@Override
					public Integer apply(BrokerRoutingInfo brokerInfo, Integer integer) {
						return integer - 1;
					}
				});
				//检查
				Set<BrokerRoutingInfo> brokerInfoSet = brokerHp.keySet();
				for (BrokerRoutingInfo brokerInfo : brokerInfoSet) {
					if (brokerHp.get(brokerInfo) < 0) {
						deleteBroker(brokerInfo);
					}
				}
			}
		}, 0, 10000);
		return BaseResponsePack.simpleSuccess();
	}

	/**
	 * 关闭 NameServer：清空缓存数据
	 */
	@GetMapping("shutdown")
	public BaseResponsePack shutdown() {
		running = false;
		topicBrokerMap.clear();
		brokerHp.clear();
		hpTimer.cancel();
		return BaseResponsePack.simpleSuccess();
	}
}
