package com.kevin.broker.service;

import com.kevin.broker.domain.DeadLetterQueue;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 20349
* @description 针对表【dead_letter_queue(存储死信消息的特殊队列(消费失败达到最大重试次数))】的数据库操作Service
* @createDate 2023-01-14 13:57:00
*/
public interface DeadLetterQueueService extends IService<DeadLetterQueue> {

}
