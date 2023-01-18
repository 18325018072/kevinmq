package com.kevin.broker.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kevin.broker.domain.DeadLetterQueue;
import com.kevin.broker.service.DeadLetterQueueService;
import com.kevin.broker.dao.mapper.DeadLetterQueueMapper;
import org.springframework.stereotype.Service;

/**
* @author 20349
* @description 针对表【dead_letter_queue(存储死信消息的特殊队列(消费失败达到最大重试次数))】的数据库操作Service实现
* @createDate 2023-01-14 13:57:00
*/
@Service
public class DeadLetterQueueServiceImpl extends ServiceImpl<DeadLetterQueueMapper, DeadLetterQueue>
    implements DeadLetterQueueService{

}




