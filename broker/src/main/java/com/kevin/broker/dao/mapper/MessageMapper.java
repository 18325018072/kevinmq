package com.kevin.broker.dao.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kevin.kevinmq.common.Message;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 20349
* @description 针对表【message(消息 存储表，用于持久化，防止宕机)】的数据库操作Mapper
* @createDate 2022-12-19 19:56:44
*/
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

}




