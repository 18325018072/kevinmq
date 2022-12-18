package com.kevin.broker.dao;

import com.kevin.broker.entry.BrokerRoutingInfo;
import com.kevin.broker.entry.pac.BaseResponsePack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

@Repository
@Slf4j
public class HttpUtil {

	@Autowired
	private RestTemplate restTemplate;
	private String nameServerUrl;

	public void setNameServerUrl(String url) {
		nameServerUrl = url;
	}

	/**
	 * 测试能否调通url
	 */
	public boolean testUrl(String url) {
		ResponseEntity<BaseResponsePack> response = restTemplate.getForEntity(url + "/testNameServer", BaseResponsePack.class);
		BaseResponsePack responseBody = response.getBody();
		return responseBody != null && responseBody.getStatus() == BaseResponsePack.SUCCESS_CODE;
	}

	/**
	 * 发送路由信息给nameServer
	 * @param brokerRoutingInfo
	 */
	public void sendInfoToNameServer(BrokerRoutingInfo brokerRoutingInfo) {
		if (nameServerUrl == null){
			return;
		}
		ResponseEntity<BaseResponsePack> response = restTemplate.postForEntity(nameServerUrl + "/updateBroker", brokerRoutingInfo, BaseResponsePack.class);
		BaseResponsePack responseBody = response.getBody();
		if (responseBody.getStatus()!=0) {
				log.error("sendHeartbeatToNameServer: "+responseBody.getInfo());
		}
	}

	public void sendShutDownToNameServer(BrokerRoutingInfo brokerRoutingInfo) {
		if (nameServerUrl == null){
			return;
		}
		ResponseEntity<BaseResponsePack> response = restTemplate.postForEntity(nameServerUrl + "/shutdownBroker", brokerRoutingInfo, BaseResponsePack.class);
		BaseResponsePack responseBody = response.getBody();
		if (responseBody.getStatus()!=0) {
				log.error("shutdown: "+responseBody.getInfo());
		}
	}
}
