package com.kevin.mqclient.dao;

import com.kevin.kevinmq.common.BaseResponsePack;
import com.kevin.kevinmq.common.BrokerRoutingInfo;
import com.kevin.mqclient.entry.ConsumerSubscribeInfoPack;
import com.kevin.kevinmq.common.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class HttpUtil {
    @Autowired
    private RestTemplate restTemplate;

    /**
     * 测试能否调通url
     */
    public boolean testUrl(String url) {
        ResponseEntity<BaseResponsePack> response = restTemplate.getForEntity(url + "/testNameServer", BaseResponsePack.class);
        BaseResponsePack responseBody = response.getBody();
        return responseBody != null && responseBody.getStatus() == BaseResponsePack.SUCCESS_CODE;
    }

    /**
     * 消费者：从 Broker 拉取消息
     */
    public List<Message> pullFromBroker(BrokerRoutingInfo targetBrokerRoutingInfo, Map<String, List<String>> subscriptionMap, long pullBatchSize) {
        String url=targetBrokerRoutingInfo.getIp()+":"+targetBrokerRoutingInfo.getPort()+"/getMessageBatch";
        ConsumerSubscribeInfoPack request=new ConsumerSubscribeInfoPack(subscriptionMap,pullBatchSize);
        BaseResponsePack response = restTemplate.postForEntity(url, request,BaseResponsePack.class).getBody();
        if (response.getStatus()==0){
            return (List<Message>) response.getObject();
        }
        return null;
    }

    /**
     * 消费者/生产者：查询 topic 对应的 broker
     */
    public Map<String, List<BrokerRoutingInfo>> getBrokersByTopics(Set<String> keySet, String nameServerUrl) {
        String url=nameServerUrl+"/getBrokerInfoByTopic";
//        restTemplate.get
        return null;
    }

    public BaseResponsePack sendSynchronously(Message msg, BrokerRoutingInfo targetBrokerRoutingInfo, String producerName, String nameServerUrl) {
        return null;
    }

    public void feedback(List<Message> resMsgList) {
    }
}
