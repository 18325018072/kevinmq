package com.kevin.mqclient.dao;

import com.kevin.mqclient.entry.BrokerRoutingInfo;
import com.kevin.mqclient.entry.Message;
import com.kevin.mqclient.entry.pac.BaseResponsePack;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class HttpUtil {
    public List<Message> pullFromBroker(BrokerRoutingInfo targetBrokerRoutingInfo, Map<String, List<String>> subscriptionMap, long pullBatchSize) {
        return null;
    }

    public Map<String, List<BrokerRoutingInfo>> getBrokersByTopics(Set<String> keySet) {
        return null;
    }

    public BaseResponsePack sendSynchronously(Message msg, BrokerRoutingInfo targetBrokerRoutingInfo, String producerName) {
        return null;
    }

    public void feedback(List<Message> resMsgList) {
    }
}
