package com.kevin.mqclient.producer;


import com.kevin.kevinmq.common.BaseResponsePack;

/**
 * 异步发送消息的回调函数
 */
public interface SendCallback {
    /**
     * 成功的回调
     * @param sendResult 返回的结果
     */
    void onSuccess(BaseResponsePack sendResult);

    /**
     * 失败的回调
     * @param e 异常
     */
    void onFail(BaseResponsePack sendResult);
}
