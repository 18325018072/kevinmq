package com.kevin.broker.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kevin.broker.dao.mapper.MessageMapper;
import com.kevin.broker.service.BrokerService;
import com.kevin.broker.service.MessageService;
import com.kevin.kevinmq.common.Message;
import org.springframework.stereotype.Service;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {
}
