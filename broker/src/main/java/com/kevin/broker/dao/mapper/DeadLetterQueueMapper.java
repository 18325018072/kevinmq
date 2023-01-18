package com.kevin.broker.dao.mapper;

import com.kevin.broker.domain.DeadLetterQueue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author 20349
 * @description 针对表【dead_letter_queue(存储死信消息的特殊队列(消费失败达到最大重试次数))】的数据库操作Mapper
 * @createDate 2023-01-14 13:57:00
 * @Entity com.kevin.broker.domain.DeadLetterQueue
 */
@Mapper
public interface DeadLetterQueueMapper extends BaseMapper<DeadLetterQueue> {

}




