package com.kevin.broker.entry.pac;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一条消息的发送结果
 * @author Kevin2
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendResult {
    private SendStatus status;
    private Long messageId;
    private String info;
}
